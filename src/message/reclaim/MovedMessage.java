package message.reclaim;

import message.ProtocolMessage;
import util.Constants;

public class MovedMessage extends ProtocolMessage {
    private int chunkNum;

    public MovedMessage(String senderId, String fileId, String chunkNum) {
        super("MOVED", senderId, fileId);
        this.chunkNum = Integer.parseInt(chunkNum);
    }

    public MovedMessage(long senderId, String fileId, int chunkNum) {
        super("MOVED", senderId, fileId);
        this.chunkNum = chunkNum;
    }

    public int getChunkNum() {
        return chunkNum;
    }

    public byte[] getBytes() {
        String msg = getInfo() + Constants.space + chunkNum + Constants.crlf + Constants.crlf;

        return msg.getBytes();
    }

    @Override
    public String toString() {
        return super.toString() + " " + chunkNum + "\n";
    }
}
