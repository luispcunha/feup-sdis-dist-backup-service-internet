package message.shift;

import message.ProtocolMessage;
import util.Constants;

public class ShiftGetChunkMessage extends ProtocolMessage {
    private int chunkNum;

    public ShiftGetChunkMessage(String senderId, String fileId, String chunkNum) {
        super("SHIFT_GETCHUNK", senderId, fileId);
        this.chunkNum = Integer.parseInt(chunkNum);
    }

    public ShiftGetChunkMessage(long senderId, String fileId, int chunkNum) {
        super("SHIFT_GETCHUNK", senderId, fileId);
        this.chunkNum = chunkNum;
    }

    public ShiftGetChunkMessage(ShiftGetChunkMessage message, long id) {
        super("SHIFT_GETCHUNK", id, message.getFileId());
        this.chunkNum = message.getChunkNum();
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
