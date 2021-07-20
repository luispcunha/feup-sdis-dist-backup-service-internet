package message.predecessor;

import chord.NodeInfo;
import message.Message;
import util.Constants;
import util.Log;

import java.io.*;

public class SendPredMessage extends Message {
    private NodeInfo nodePredecessor;

    public SendPredMessage(String id, byte[] predecessor){
        super("PREDECESSOR", id);

        ByteArrayInputStream in = new ByteArrayInputStream(predecessor);
        try {
            ObjectInputStream bodyIn = new ObjectInputStream(in);
            nodePredecessor = (NodeInfo) bodyIn.readObject();
            in.close();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println(e);
            Log.logError(e.getMessage());
        }
    }

    public SendPredMessage(long id, NodeInfo predecessor){
        super("PREDECESSOR", id);
        this.nodePredecessor = predecessor;
    }

    public NodeInfo getNodePredecessor() {
        return nodePredecessor;
    }

    @Override
    public byte[] getBytes() {
        String msg = getInfo() + Constants.crlf + Constants.crlf;
        byte[] msgBytes = new byte[0];

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(msg.getBytes());
            ObjectOutputStream bodyOut = new ObjectOutputStream(out);
            bodyOut.writeObject(nodePredecessor);
            msgBytes = out.toByteArray();

            out.close();
        } catch (IOException e) {
            System.err.println(e);
            Log.logError(e.getMessage());
        }

        return msgBytes;
    }

    @Override
    public String toString() {
        return super.toString() + Constants.space + (nodePredecessor == null ? "null" : nodePredecessor.toString());
    }
}

