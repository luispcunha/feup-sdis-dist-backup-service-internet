package message.successor;

import chord.NodeInfo;
import message.Message;
import util.Constants;
import util.Log;

import java.io.*;

public class SendSuccMessage extends Message {
    private NodeInfo nodeSuccessor;
    private long nodeKey;

    public SendSuccMessage(String id, String key, byte[] successor){
        super("SUCCESSOR", id);

        ByteArrayInputStream in = new ByteArrayInputStream(successor);
        try {
            ObjectInputStream inObj = new ObjectInputStream(in);
            this.nodeSuccessor = (NodeInfo) inObj.readObject();
        } catch (Exception e) {
            System.err.println(e);
            Log.logError(e.getMessage());
        }

        this.nodeKey = Long.parseLong(key);
    }

    public SendSuccMessage(long id, String key, NodeInfo successor){
        super("SUCCESSOR", id);
        this.nodeSuccessor = successor;
        this.nodeKey = Long.parseLong(key);
    }

    public NodeInfo getNodeSuccessor() {
        return nodeSuccessor;
    }

    public long getKey() {
        return nodeKey;
    }

    @Override
    public byte[] getBytes() {
        String msg = getInfo() + Constants.space + nodeKey + Constants.crlf + Constants.crlf;
        byte[] msgBytes = new byte[0];

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(msg.getBytes());
            ObjectOutputStream bodyOut = new ObjectOutputStream(out);
            bodyOut.writeObject(nodeSuccessor);
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
        return super.toString() + Constants.space + nodeKey + Constants.space + (nodeSuccessor == null ? "null" : nodeSuccessor.toString());
    }
}
