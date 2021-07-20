package protocol;

import file.Chunk;
import file.ChunkKey;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkRestoreSynchronizer {
   // received CHUNKs
    private ConcurrentHashMap<String, Set<Chunk>> receivedChunks;

    public ChunkRestoreSynchronizer() {
        this.receivedChunks = new ConcurrentHashMap<>();
    }

    public void chunkReceived(Chunk chunk) {
        // if this peer is the one restoring the file related to this chunk
        Set<Chunk> chunks = receivedChunks.get(chunk.getFileID());
        if (chunks == null)
            return;

        synchronized (chunks) {
            chunks.add(chunk);
        }
    }

    public void restoreFile(String fileID) {
        receivedChunks.putIfAbsent(fileID, new HashSet<Chunk>());
    }

    public boolean isRestoringFile(String fileID) {
        return receivedChunks.containsKey(fileID);
    }

    /**
     * Check if all chunks of a file have been received
     */
    public List<Chunk> allChunksReceived(String fileID) {
        Set<Chunk> chunks = receivedChunks.get(fileID);

        if (chunks == null)
            return null;

        synchronized (chunks) {
            List<Integer> chunkNumbers = new ArrayList<>();
            for (Chunk c : chunks) {
                chunkNumbers.add(c.getNumber());
                if (c.getNumber() == chunks.size() - 1)
                    if (c.getSize() == Chunk.MAX_SIZE)
                        return null;
            }

            Collections.sort(chunkNumbers);

            for (int i = 0; i < chunkNumbers.size(); i++)
                if (chunkNumbers.get(i) != i)
                    return null;

            return new ArrayList<>(receivedChunks.remove(fileID));
        }
    }
}