package chord;

import message.Message;
import message.notify.NotifyMessage;
import message.ping.PingMessage;
import message.predecessor.GetPredMessage;
import message.predecessor.SendPredMessage;
import message.successor.GetListMessage;
import message.successor.GetSuccMessage;
import message.successor.SendListMessage;
import message.successor.SendSuccMessage;

import java.util.LinkedList;

public class ChordProtocol {

    private ChordNode thisNode;

    public ChordProtocol(ChordNode node){
        this.thisNode = node;
    }

    public NodeInfo findSuccessor(NodeInfo receiverNode, long key) {
        Message request = new GetSuccMessage(thisNode.getID(), key);

        Message reply = thisNode.sendRequest(request, receiverNode);

        if(reply == null || !reply.getMessageType().equals("SUCCESSOR")){
            //System.out.println("Suppose to be SUCCESSOR was " + (reply == null ? "null" : reply.toString()));
            return null;
        }

        return ((SendSuccMessage) reply).getNodeSuccessor();
    }

    public NodeInfo findPredecessor(NodeInfo node) {
        Message request = new GetPredMessage(thisNode.getID());

        Message reply = thisNode.sendRequest(request, node);

        if(reply == null || !reply.getMessageType().equals("PREDECESSOR")){
            //System.out.println("Suppose to be PREDECESSOR was " + (reply == null ? "null" : reply.toString()));
            return null;
        }

        return ((SendPredMessage) reply).getNodePredecessor();
    }

    public LinkedList<NodeInfo> getSuccessorsList(NodeInfo node) {
        Message request = new GetListMessage(thisNode.getID());

        Message reply = thisNode.sendRequest(request, node);

        if(reply == null || !reply.getMessageType().equals("LIST")){
            //System.out.println("Suppose to be LIST was " + (reply == null ? "null" : reply.toString()));
            return null;
        }

        return ((SendListMessage) reply).getNodeSuccessors();
    }

    public void notify(NodeInfo senderNode, NodeInfo receiverNode) {
        Message request = new NotifyMessage(thisNode.getID(), senderNode.getIpAddress(), senderNode.getChordPort(), senderNode.getPeerPort());

        thisNode.sendMessage(request, receiverNode);
    }

    public boolean ping(NodeInfo node) {
        Message request = new PingMessage(thisNode.getID());

        Message reply = thisNode.sendRequest(request, node);

        if(reply == null || !reply.getMessageType().equals("PONG")){
            //System.out.println("Suppose to be PONG was " + (reply == null ? "null" : reply.toString()));
        }

        return reply != null && reply.getSenderId() == node.getId();
    }
}
