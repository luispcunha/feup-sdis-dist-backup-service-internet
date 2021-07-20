package filesystem;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Information regarding the chunks of a file which are being backed up by a peer.
 */
public class StoredInfo implements Serializable {

    private static final long serialVersionUID = 6564061166107827209L;

    private ConcurrentHashMap<Integer, ChunkInfo> chunks;

    public StoredInfo() {
        chunks = new ConcurrentHashMap<>();
    }

    public ConcurrentHashMap<Integer, ChunkInfo> getChunks() {
        return chunks;
    }

    public int getChunkPerceivedRepDegree(int chunkNo) {
        ChunkInfo chunkInfo = chunks.get(chunkNo);

        if (chunkInfo == null)
            return -1;
        else
            return chunkInfo.getRepDegree();
    }

    public boolean hasChunks() {
        return ! chunks.isEmpty();
    }

    public boolean isStored(int chunkNo) {
        return chunks.containsKey(chunkNo);
    }

    public boolean addChunk(ChunkInfo chunkInfo) {
        return chunks.putIfAbsent(chunkInfo.getChunkNo(), chunkInfo) == null;
    }

    public void removeChunk(int chunkNo) {
        chunks.remove(chunkNo);
    }

    public boolean addPeerBackingUpChunk(int chunkNo) {
        ChunkInfo chunkInfo = chunks.get(chunkNo);

        if (chunkInfo == null)
            return false;

        chunkInfo.incRepDegree();

        return true;
    }

    public boolean removePeerBackingUpChunk(int chunkNo, long peerId) {
        ChunkInfo chunkInfo = chunks.get(chunkNo);

        if (chunkInfo == null)
            return false;

        chunkInfo.decRepDegree();

        return true;
    }

    public ChunkInfo getChunk(int chunkNum) {
        return chunks.get(chunkNum);
    }

    @Override
    public String toString() {
        String ret = "";
        for (ConcurrentHashMap.Entry<Integer, ChunkInfo> entry : chunks.entrySet()) {
            ret += "    Chunk No : " + entry.getKey() + "\n";
            ret += entry.getValue().toString();
        }
        return ret;
    }
}
