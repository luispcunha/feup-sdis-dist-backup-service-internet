package file;

import java.io.Serializable;
import java.util.Objects;

public class ChunkKey implements Serializable {
    private final String fileId;
    private final int chunkNo;
    private long chordKey;

    public ChunkKey(String fileId, int chunkNo) {
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        try {
            this.chordKey = ChunkIDGenerator.generateID(fileId, chunkNo);
        } catch (Exception e) {

        }
    }

    public String getFileID() {
        return this.fileId;
    }

    public int getNumber() {
        return this.chunkNo;
    }

    public long getChordKey() {
        return chordKey;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileId, chunkNo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true; // are the references equal
        if (o == null)
            return false; // is the other object null
        if (getClass() != o.getClass())
            return false; // both objects the same class

        ChunkKey cKey = (ChunkKey) o; // cast the other object

        return fileId.equals(cKey.getFileID()) && chunkNo == cKey.getNumber(); // actual comparison
    }
}