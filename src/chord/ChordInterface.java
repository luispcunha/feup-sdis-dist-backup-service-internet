package chord;

import java.net.UnknownHostException;

public interface ChordInterface {
    static boolean intervalID(long minID, long id, long maxID) {
        if (minID < maxID)
            return minID < id && id < maxID;
        else
            return minID < id || id < maxID;
    }

    NodeInfo lookup(long id);
    long getID();
    void create();
    void join(NodeInfo node) throws UnknownHostException;
    NodeInfo getSuccessor();
}
