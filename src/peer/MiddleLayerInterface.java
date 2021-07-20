package peer;

import chord.NodeInfo;

public interface MiddleLayerInterface {
    void newPredecessorCallback(NodeInfo predecessor);
    void newSuccessorCallback(NodeInfo successor);
}
