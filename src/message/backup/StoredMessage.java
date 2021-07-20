package message.backup;

import message.ProtocolMessage;
import util.Constants;

public class StoredMessage extends ProtocolMessage {
    private int chunkNum;

    public StoredMessage(String senderId, String fileId, String chunkNum) {
        super("STORED", senderId, fileId);
        this.chunkNum = Integer.parseInt(chunkNum);
    }

    public StoredMessage(long senderId, String fileId, int chunkNum) {
        super("STORED", senderId, fileId);
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