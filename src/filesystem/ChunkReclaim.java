package filesystem;


public class ChunkReclaim implements Comparable<ChunkReclaim> {
    private int chunkNo, repDegree, size;
    private String fileId;

    public ChunkReclaim(String fileId, int chunkNo, int repDegree, int size) {
        this.fileId = fileId;
        this.chunkNo = chunkNo;
        this.repDegree = repDegree;
        this.size = size;
    }

    @Override
    public int compareTo(ChunkReclaim chunk) {
        if (this.repDegree > chunk.repDegree)
            return -1;
        else if (this.repDegree < chunk.repDegree)
            return 1;

        if (this.size > chunk.size)
            return -1;
        else if (this.size < chunk.size)
            return 1;

        return 0;
    }

    public String getFileId() {
        return this.fileId;
    }

    public int getChunkNo() {
        return this.chunkNo;
    }

    public int getSize() {
        return this.size;
    }

    public int getRepDegree() {
        return this.repDegree;
    }
}