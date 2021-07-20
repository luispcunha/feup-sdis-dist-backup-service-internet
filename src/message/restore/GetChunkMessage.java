package message.restore;

import message.ProtocolMessage;
import util.Constants;

public class GetChunkMessage extends ProtocolMessage {
    private int chunkNum;

    public GetChunkMessage(String senderId, String fileId, String chunkNum) {
        super("GETCHUNK", senderId, fileId);
        this.chunkNum = Integer.parseInt(chunkNum);
    }

    public GetChunkMessage(long senderId, String fileId, int chunkNum) {
        super("GETCHUNK", senderId, fileId);
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
