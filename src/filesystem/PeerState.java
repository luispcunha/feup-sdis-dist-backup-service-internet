package filesystem;

import chord.ChordInterface;
import file.ChunkKey;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Information about a peers' state, including
 * the files for which it has requested backup
 * and the chunks it is currently backing up.
 */
public class PeerState implements Serializable {

    private static final long serialVersionUID = -4914787634980631385L;

    // files for which the peer has initiated backup
    private Map<String, FileInfo> backupFiles;
    // information about the chunks the peer has stored
    private Map<String, StoredInfo> storedChunks;
    // files for which this peer has requested backup that have been deleted but are still stored in other peers
    private Map<Long, Set<String>> undeletedFiles;

    // key: fileId + chunkNum  |  value: replicaton degree
    private Map<ChunkKey, Integer> undeletedChunks;

    // table that contains all redirects
    private Map<ChunkKey, Long> redirectTable;

    private long maxStorage;
    private long usedStorage;

    // true when the state has been modified since last save
    private volatile boolean modified;

    public PeerState() {
        backupFiles = new ConcurrentHashMap<>();
        storedChunks = new ConcurrentHashMap<>();
        undeletedFiles = new ConcurrentHashMap<>();
        undeletedChunks = new ConcurrentHashMap<>();

        redirectTable = new ConcurrentHashMap<>();

        maxStorage = Integer.MAX_VALUE;
        usedStorage = 0;

        modified = true;
    }

    /**
     * Get the modified flag
     * @return true if the state has been modified
     */
    public boolean modified() {
        return modified;
    }

    /**
     * Sets the state modified flag to false after it has been saved to disk
     */
    public void savedToDisk() {
        modified = false;
    }

    /**
     * Calculates the available space on the peer's filesystem
     * @return Returns a long that represents the available space
     */
    public synchronized long getAvailableSpace() {
        return maxStorage - usedStorage;
    }

    // removes the information about a previously stored chunk

    /**
     * Removes the information about a chunk that the peer has stored, this includes updating the used space
     * @param fileId    file id of the file to which the chunk belongs
     * @param chunkNo   chunk number
     * @param size      chunk size
     */
    public void removeStoredChunk(String fileId, int chunkNo, int size) {
        StoredInfo storedFileInfo = this.storedChunks.get(fileId);
        if (storedFileInfo == null)
            return;

        storedFileInfo.removeChunk(chunkNo);

        synchronized (this) {
            this.usedStorage -= size;
        }

        if (! storedFileInfo.hasChunks())
            this.storedChunks.remove(fileId);

        modified = true;
    }

    /**
     * Gets the peers that currently have the specified chunk stored, 
     * in case this information cannot be found returns null
     * @param fileId    file id of the file to which the chunk belongs
     * @param chunkNo   chunk number
     * @return The list of peer ids, null if the chunk is not present
     */
    /*public List<Long> getBackupChunkPeers(String fileId, int chunkNo) {
        FileInfo fileInfo = backupFiles.get(fileId);
        if (fileInfo == null)
            return null;

        return fileInfo.getChunkPeers(chunkNo);
    }*/

    // returns the perceived replication degree of the specified chunk
    /**
     * Get the perceived replication degree of a chunk for which the peer has initiated backup
     * @param fileId    file id
     * @param chunkNo   chunk number
     * @return an int that represents the degree, null in case the info is not present
     */
    public int getBackupChunkPerceivedRepDegree(String fileId, int chunkNo) {
        FileInfo fileInfo = backupFiles.get(fileId);

        if (fileInfo == null)
            return 0;

        return fileInfo.getChunkRepDegree(chunkNo);
    }

    /**
     * Check if a peer currently has stored chunks of a file
     * @param fileId file id
     * @return true if any information is stored, false otherwise
     */
    public boolean isStoredFile(String fileId) {
        return storedChunks.containsKey(fileId);
    }

    /**
     * Check if the peer has initiated the backup of the specified file
     * @param fileId file id
     * @return true if it has, false otherwise
     */
    public boolean isBackupFile(String fileId) {
        return backupFiles.containsKey(fileId);
    }

    /**
     * Check if the peer currently has a specified chunk stored
     * @param fileId    file id
     * @param chunkNo   chunk number
     * @return true if it has, false otherwise
     */
    public boolean isStoredChunk(String fileId, int chunkNo) {
        StoredInfo storedFileInfo = storedChunks.get(fileId);

        if (storedFileInfo == null)
            return false;

        return storedFileInfo.isStored(chunkNo);
    }

    /**
     * Deletes all the information about a file that the peer has initiated the backup.
     * Sets the modified flag to true.
     * @param fileId file id
     */
    public List<Integer> deleteBackupFile(String fileId) {
        FileInfo info = backupFiles.remove(fileId);
        List<Integer> chunks = info.getChunks();

        for (int chunkNum : chunks) {
            this.undeletedChunks.putIfAbsent(new ChunkKey(fileId, chunkNum), info.getChunkRepDegree(chunkNum));
        }

        modified = true;
        return chunks;
    }

    public void removeUndeletedChunk(String fileId, int chunkNum) {
        Integer repDegree = this.undeletedChunks.get(new ChunkKey(fileId, chunkNum));

        // TODO: in this case the peer received a deleted for a chunk that should already be deleted
        // not sure what to do in this case
        if (repDegree == null) {
            modified = true;
            return;
        }
        else if (repDegree-- > 0) {
            modified = true;
            return;
        }

        this.undeletedChunks.remove(new ChunkKey(fileId, chunkNum));
        modified = true;
    }

    /**
     * Updates the map of peers that have undeleted files. In case there is no entry for the peer, 
     * a new one is created and the file id is added to the set of undeleted files present on that
     * peer. In case an entry already exists, simply adds the file id to the set.
     * This method sets the modified flag to true.
     * @param peerID peer id
     * @param fileID file id
     */
    public void addUndeletedFile(long peerID, String fileID) {
        undeletedFiles.putIfAbsent(peerID, new HashSet<>());

        Set<String> chunks = undeletedFiles.get(peerID);
        synchronized (chunks) {
            chunks.add(fileID);
        }

        modified = true;
    }

    /**
     * Removes a file id from the undeleted files for all peers
     * @param fileId file id
     */
    public void removeUndeletedFile(String fileId) {
        for (Set<String> files : undeletedFiles.values()) {
            synchronized (files) {
                files.remove(fileId);
            }
        }
    }

    /**
     * Get a peer's set of undeleted file's ids 
     * @param peerID peer id
     * @return set of file ids
     */
    public Set<String> getUndeletedFilesFrom(long peerID) {
        return undeletedFiles.get(peerID);
    }

    /**
     * Get the file ids of all undeleted files
     * @return set of file ids
     */
    public Set<String> getUndeletedFiles() {
        Set<String> allFiles = new HashSet<>();

        for (Set<String> files : undeletedFiles.values()) {
            for (String file : files) {
                allFiles.add(file);
            }
        }

        return allFiles;
    }
    
    /**
     * Delete a file id from the undeleted file
     * @param peerID
     * @param fileID
     */
    public void peerDeletedFile(long peerID, String fileID) {
        Set<String> files = undeletedFiles.get(peerID);

        if (files == null)
            return;

        synchronized (files) {
            files.remove(fileID);
            if (files.isEmpty())
                undeletedFiles.remove(peerID);
        }

        modified = true;
    }
    
    /**
     * Delete all the information a peer has stored about a file
     * @param fileId file id
     */
    public void deleteStoredFile(String fileId) {
        StoredInfo info = storedChunks.remove(fileId);

        if (info == null)
            return;

        ConcurrentHashMap<Integer, ChunkInfo> chunks = info.getChunks();
        chunks.values().forEach((chunk) -> {
            synchronized (this) {
                usedStorage -= chunk.getSize();
            }
        });

        modified = true;
    }

    // when the backup of a file is initiated

    /**
     * Add information about a file for which the backup was initiated.
     * Sets the modified flag to true.
     * @param path          file path
     * @param fileId        file id
     * @param repDegree     desired replication degree
     * @return  true if the information was successfully added, false if an entry already existed
     */
    public boolean insertFileInfo(String path, String fileId, int repDegree) {
        modified = true;
        boolean result = null == backupFiles.putIfAbsent(fileId, new FileInfo(path, fileId, repDegree));

        if (result) {
            removeUndeletedFile(fileId);
        }

        return result;
    }

    // for files that the peer initiated the backup
    /**
     * Update information about a file for which the peer initiated the backup.
     * Sets the modified flag to true.
     * @param fileId        file id
     * @param chunkNo       chunk number
     * @param senderId      sender id
     * @return true if an entry for the file already exists, false otherwise
     */
    public boolean addFileInfo(String fileId, int chunkNo, long senderId) {
        modified = true;
        FileInfo info = backupFiles.get(fileId);

        if (info == null)
            return false;

        info.addChunk(chunkNo, senderId);
        return true;
    }

    /**
     * Removes all information of a file for which the peer initiated the backup
     * Sets the modified flag to true.
     * @param fileId        file id
     * @param chunkNo       chunk number
     * @param senderId      sender id
     * @return true if an entry for the file existed, false otherwise
     */
    public boolean removeFileInfo(String fileId, int chunkNo, long senderId) {
        modified = true;
        FileInfo info = backupFiles.get(fileId);

        if (info == null)
            return false;

        return info.removeChunk(chunkNo, senderId);
    }

    /**
     * Fetches the information of a file
     * @param fileId        file id
     * @return FileInfo of the specified file
     */
    public FileInfo getFileInfo(String fileId) {
        return backupFiles.get(fileId);
    }
    
    /**
     * Add information about a chunk that the peer is going store.
     * Sets the modified flag to true.
     * @param fileId        file id
     * @param repDegree     desired replication degree
     * @param chunkNo       chunk number
     * @param size          chunk size
     * @return true if the chunk information was added successfully, false if an entry for the chunk already existed
     */
    public boolean addStoredChunkInfo(String fileId, int repDegree, int chunkNo, int size) {
        modified = true;
        storedChunks.putIfAbsent(fileId, new StoredInfo());

        if (storedChunks.get(fileId).addChunk(new ChunkInfo(fileId, chunkNo, size, repDegree))) {
            synchronized (this) {
                usedStorage += size;
            }
            return true;
        } else
            return false;
    }

    /**
     * Update the list of peers that are storing a chunk that the current peer is also storing
     * Sets the modified flag to true.
     * @param fileId        file id
     * @param chunkNo       chunk number
     * @param senderId      sender id
     * @return true if the list was updated, false if the chunk is not stored on the peer
     */
    public boolean addPeerBackingUpStoredChunk(String fileId, int chunkNo, long senderId) {
        modified = true;
        StoredInfo info = storedChunks.get(fileId);
        if (info == null)
            return false;

        return info.addPeerBackingUpChunk(chunkNo);
    }

    /**
     * Remove a peer from the list of peers that are storing a chunk that the current peer is storing.
     * Sets the modified flag to true.
     * @param fileId        file id
     * @param chunkNo       chunk number
     * @param senderId      sender id
     * @return true if the peer was removed from the list, false if the chunk is not stored on the peer
     */
    public boolean removePeerBackingUpStoredChunk(String fileId, int chunkNo, long senderId) {
        modified = true;
        StoredInfo info = storedChunks.get(fileId);
        if (info == null)
            return false;

        return info.removePeerBackingUpChunk(chunkNo, senderId);
    }

    /**
     * Gets the desired replication degree of a file that the peer has stored.
     * @param fileId    file id
     * @return the replication degree if the peer stores any chunk of that file, -1 otherwise
     */
    /*
    public int getStoredFileDesiredRepDegree(String fileId) {
        StoredInfo info = storedChunks.get(fileId);
        if (info == null)
            return -1;

        return info.getRepDegree();
    }
    */


    public List<ChunkInfo> getAllStoredChunks() {
        List<ChunkInfo> result = new ArrayList<>();

        for(Map.Entry<String, StoredInfo> entry : storedChunks.entrySet()) {
            for(Map.Entry<Integer, ChunkInfo> chunks : entry.getValue().getChunks().entrySet()) {
                result.add(chunks.getValue());
            }
        }

        return result;
    }

    public ConcurrentHashMap<Integer, ChunkInfo> getStoredChunks(String fileId) {
        StoredInfo fileInfo = storedChunks.get(fileId);
        if (fileInfo == null)
            return new ConcurrentHashMap<>();

        return fileInfo.getChunks();
    }

    /**
     * Sets the replication degree of a stored chunk. Sets the modified flag to true
     * @param fileId        File identifier
     * @param chunkNum      Chunk number
     * @return  Updated replication degree, -1 in case of error.
     */
    public int setStoredChunkRepDegree(String fileId, int chunkNum, int repDegree) {
        StoredInfo fileInfo = storedChunks.get(fileId);
        if (fileInfo == null)
            return -1;

        ChunkInfo chunkInfo = fileInfo.getChunk(chunkNum);
        if (chunkInfo == null)
            return -1;

        modified = true;
        return chunkInfo.setRepDegree(repDegree);
    }

    /**
     * Decrements the replication degree of a stored chunk. Sets the modified flag to true.
     * @param fileId        File identifier
     * @param chunkNum      Chunk number
     * @return  Updated replication degree, -1 in case of error.
     */
    public int decStoredChunkRepDegree(String fileId, int chunkNum) {
        StoredInfo fileInfo = storedChunks.get(fileId);
        if (fileInfo == null)
            return -1;

        ChunkInfo chunkInfo = fileInfo.getChunk(chunkNum);
        if (chunkInfo == null)
            return -1;

        modified = true;
        return chunkInfo.decRepDegree();
    }

    /**
     * Gets the perceived replication degree of a chunk that the peer has stored.
     * @param fileId    file id
     * @param chunkNo   chunk number
     * @return the replication degree if the peer has the chunk stored, -1 otherwise
     */
    public int getStoredChunkPerceivedRepDegree(String fileId, int chunkNo) {
        StoredInfo info = storedChunks.get(fileId);
        if (info == null)
            return 0;

        return info.getChunkPerceivedRepDegree(chunkNo);
    }

    /**
     * Removes a stored chunk from the peer's state
     * @param fileId    file id
     * @param chunkNum  chunk number
     * @return 0 if successful, 1 if the chunk was not present
     */
    public void removeStoredChunk(String fileId, int chunkNum) {
        StoredInfo info = storedChunks.get(fileId);
        if (info == null)
            return;

        int size = info.getChunk(chunkNum).getSize();

        this.removeStoredChunk(fileId, chunkNum, size);
    }

    /**
     * Fetches chunks stored in this peer that belong to its predecessor.
     *
     * @return an ArrayList with the chunks in the given condition.
     */
    public List<ChunkInfo> getPredecessorChunks(long predKey, long thisID) {
        List<ChunkInfo> result = new ArrayList<>();

        for(ConcurrentHashMap.Entry<String, StoredInfo> entry : storedChunks.entrySet()) {
            for(ConcurrentHashMap.Entry<Integer, ChunkInfo> chunk : entry.getValue().getChunks().entrySet()) {
                if(ChordInterface.intervalID(thisID, chunk.getValue().getChordKey(), predKey))
                    result.add(chunk.getValue());
            }
        }

        return result;
    }

    /**
     * Fetches chunks redirected by this peer that belong to its predecessor.
     *
     * @return a Map with the chunks and peers in the given condition.
     */
    public Map<ChunkKey, Long> getPredecessorRedirectPeers(long predKey, long thisID) {
        Map<ChunkKey, Long> result = new HashMap<>();

        for(ConcurrentHashMap.Entry<ChunkKey, Long> entry : redirectTable.entrySet()) {
            if(ChordInterface.intervalID(thisID, entry.getKey().getChordKey(), predKey))
                result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    @Override
    public String toString() {
        String ret = "";
        // storage info
        ret += "Storage : " + usedStorage + " / " + maxStorage + " bytes\n";
        ret += "\n";

        // backed up files info
        ret += "Backed up files : \n";
        for (ConcurrentHashMap.Entry<String, FileInfo> entry : backupFiles.entrySet())
            ret += entry.getValue().toString();
        ret += "\n";

        // stored chunks info
        ret += "Stored chunks : \n";
        for (ConcurrentHashMap.Entry<String, StoredInfo> entry : storedChunks.entrySet()) {
            ret += "  File ID : " + entry.getKey() + "\n";
            ret += entry.getValue().toString();
        }

        if (!undeletedFiles.isEmpty()) {
            ret += "\nFiles not deleted : \n";
            for (ConcurrentHashMap.Entry<Long, Set<String>> entry : undeletedFiles.entrySet()) {
                ret += "  Peer " + entry.getKey() + " :\n";
                for (String file : entry.getValue()) {
                    ret += "    " + file + "\n";
                }
            }
        }

        if (!redirectTable.isEmpty()) {
            ret += "\nRedirected chunks : \n";
            for (ConcurrentHashMap.Entry<ChunkKey, Long> entry : redirectTable.entrySet()) {
                ChunkKey key = entry.getKey();
                ret += String.format("  File ID : %s | Chunk No : %d | Chord Key : %d | Redirect To : %d\n",
                        key.getFileID(), key.getNumber(), key.getChordKey(), entry.getValue());
            }
        }

        return ret;
    }

    /**
     * Algorithm for the reclaim protocol, decides which chunks have to be removed so that the peer maximum storage
     * capacity is updated. The chunks that have a greater replication degree are deleted firstly, amongst those the
     * largest are deleted.
     * @param newMaxStorage     Maximum storage
     * @return a list of chunks to be removed
     */
    public List<ChunkInfo> reclaim(int newMaxStorage) {

        synchronized (this) {
            this.maxStorage = newMaxStorage;
            if (this.usedStorage <= this.maxStorage)
                return new ArrayList<>();
        }

        PriorityQueue<ChunkReclaim> queue = new PriorityQueue<>();

        for (ConcurrentHashMap.Entry<String, StoredInfo> entry : storedChunks.entrySet()) {
            String fileId = entry.getKey();
            for (ConcurrentHashMap.Entry<Integer, ChunkInfo> chunk : entry.getValue().getChunks().entrySet()) {
                int chunkNo = chunk.getKey();
                int repDegree = chunk.getValue().getRepDegree();
                int size = chunk.getValue().getSize();

                queue.add(new ChunkReclaim(fileId, chunkNo, repDegree, size));
            }
        }

        List<ChunkInfo> chunksToRemove = new ArrayList<>();

        while (this.usedStorage > this.maxStorage || newMaxStorage == 0) {
            ChunkReclaim chunk = queue.poll();

            if(chunk == null) break;

            chunksToRemove.add(new ChunkInfo(chunk.getFileId(), chunk.getChunkNo(), chunk.getSize(), chunk.getRepDegree()));
            removeStoredChunk(chunk.getFileId(), chunk.getChunkNo(), chunk.getSize());
        }

        return chunksToRemove;
    }



    // ==================================================
    //                      REDIRECTS
    // ==================================================

    public void addRedirect(ChunkKey chunk, long peer) {
        this.redirectTable.putIfAbsent(chunk, peer);
        this.modified = true;
    }

    public Long getRedirect(ChunkKey chunk) {
        return this.redirectTable.get(chunk);
    }

    public Long removeRedirect(ChunkKey chunk) {
        long ret = this.redirectTable.remove(chunk);
        this.modified = true;
        return ret;
    }

    /**
     * Fetches all redirects registered on this peer
     *
     * @return Set of entries in the redirect table
     */
    public Set<Map.Entry<ChunkKey, Long>> getAllRedirects() {
        return this.redirectTable.entrySet();
    }
}