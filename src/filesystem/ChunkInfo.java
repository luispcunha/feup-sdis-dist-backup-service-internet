package filesystem;

import file.ChunkKey;

import java.io.Serializable;

/**
 * Information regarding a chunk being backed up by a peer.
 */
public class ChunkInfo implements Serializable {

    private static final long serialVersionUID = 7493772498312054194L;

    private final ChunkKey chunkKey;
    private final int size; // size of the chunk in bytes
    private int repDegree; // other peers that are currently backing up this chunk

    public ChunkInfo(String fileID, int chunkNo, int size, int repDegree) {
        this.chunkKey = new ChunkKey(fileID, chunkNo);
        this.size = size;
        this.repDegree = repDegree;
    }

    public int getSize() {
        return this.size;
    }

    public synchronized int incRepDegree() {
        return ++this.repDegree;
    }

    public synchronized int decRepDegree() {
        return --this.repDegree;
    }

    public synchronized int setRepDegree(int repDegree) {
        this.repDegree = repDegree;
        return this.repDegree;
    }

    public synchronized int getRepDegree() {
        return this.repDegree;
    }

    public String getFileID() {
        return chunkKey.getFileID();
    }

    public int getChunkNo() {
        return chunkKey.getNumber();
    }

    public long getChordKey() {
        return chunkKey.getChordKey();
    }

    public ChunkKey getKey() {
        return chunkKey;
    }

    @Override
    public String toString() {
        String ret = "";
        ret += "      Perceived RD : " + this.repDegree + "\n";
        ret += "      Size : " + this.size + " bytes\n";
        ret += "      ChordKey : " + this.chunkKey.getChordKey() + "\n";

        return ret;
    }
}