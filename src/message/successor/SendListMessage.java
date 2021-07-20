package message.successor;

import chord.NodeInfo;
import message.Message;
import util.Constants;
import util.Log;

import java.io.*;
import java.util.LinkedList;

public class SendListMessage extends Message {
    private LinkedList<NodeInfo> nodeSuccessors;

    public SendListMessage(String id, byte[] successorsList){
        super("LIST", id);

        //System.out.println("Weird(" + successorsList.length + "): " + Arrays.toString(successorsList));

        ByteArrayInputStream in = new ByteArrayInputStream(successorsList);
        try {
            ObjectInputStream inObj = new ObjectInputStream(in);
            this.nodeSuccessors = (LinkedList<NodeInfo>) inObj.readObject();
        } catch (Exception e) {
            System.err.println(e);
            Log.logError(e.getMessage());
        }
    }

    public SendListMessage(long id, LinkedList<NodeInfo> successorsList){
        super("LIST", id);
        this.nodeSuccessors = successorsList;
    }

    public LinkedList<NodeInfo> getNodeSuccessors() {
        return nodeSuccessors;
    }

    @Override
    public byte[] getBytes() {
        String msg = getInfo() + Constants.crlf + Constants.crlf;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(msg.getBytes());
            ObjectOutputStream bodyOut = new ObjectOutputStream(out);
            bodyOut.writeObject(nodeSuccessors);
        } catch (IOException e) {
            System.err.println(e);
            Log.logError(e.getMessage());
        }

        return out.toByteArray();
    }

    @Override
    public String toString() {
        String result = super.toString();
        for(NodeInfo node : nodeSuccessors) {
            result += "\n\t" + (node == null ? "null" : node.toString());
        }

        return result;
    }
}

