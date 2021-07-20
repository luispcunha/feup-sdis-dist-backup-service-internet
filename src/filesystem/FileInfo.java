package filesystem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Information regarding a file for which a peer requested backup.
 */
public class FileInfo implements Serializable {
    private static final long serialVersionUID = 7911687701238652479L;

    private final String path;          // path to file
    private final String id;            // file id
    private final int desiredRepDegree; // desired replication degree

    // key: chunkNum | value: replication degree
    private ConcurrentHashMap<Integer, Integer> chunks;


    public FileInfo(String path, String id, int repDegree) {
        this.path = path;
        this.id = id;
        this.desiredRepDegree = repDegree;
        this.chunks = new ConcurrentHashMap<>();
    }

    public void addChunk(int chunkNo, long peerID) {
        chunks.putIfAbsent(chunkNo, 0);
        int repDegree = chunks.get(chunkNo);
        repDegree++;

        chunks.put(chunkNo, repDegree);
    }

    public boolean removeChunk(int chunkNo, long peerID) {
        boolean removed = false;
        Integer repDegree = chunks.get(chunkNo);

        if (repDegree != null) {
            repDegree--;
            chunks.put(chunkNo, repDegree);
            removed = true;
        }

        return removed;
    }

    public int getChunkRepDegree(int chunkNum) {
        Integer repDegree = chunks.get(chunkNum);

        if (repDegree == null)
            return 0;

        return repDegree;
    }

    /*public List<Long> getChunkPeers(int chunkNo) {
        Set<Long> set = chunks.get(chunkNo);
        if (set == null)
            return null;
        return new ArrayList<>(set);
    }*/

    public List<Integer> getChunks() {
        return new ArrayList<>(chunks.keySet());
    }

    @Override
    public String toString() {
        String ret = "";
        ret += "  ID : " + id + "\n";
        ret += "  Path : " + path + "\n";
        ret += "  Desired RD : " + desiredRepDegree + "\n";

        for (ConcurrentHashMap.Entry<Integer, Integer> entry : chunks.entrySet()) {
            ret += String.format("    Chunk No : %d | Perceived RD : %d\n", entry.getKey(), entry.getValue());
        }

        return ret;
    }
}