package message.shift;

import message.ProtocolMessage;
import util.Constants;

public class ShiftMessage extends ProtocolMessage {
    private int chunkNum;
    private int repDegree;

    public ShiftMessage(String senderId, String fileId, String chunkNum, String repDegree) {
        super("SHIFT", senderId, fileId);
        this.chunkNum = Integer.parseInt(chunkNum);
        this.repDegree = Integer.parseInt(repDegree);
    }

    public ShiftMessage(long senderId, String fileId, int chunkNum, int repDegree) {
        super("SHIFT", senderId, fileId);
        this.chunkNum = chunkNum;
        this.repDegree = repDegree;
    }

    public int getChunkNum() {
        return chunkNum;
    }
    public int getRepDegree() {
        return repDegree;
    }

    public byte[] getBytes() {
        String msg = getInfo() + Constants.space + chunkNum + Constants.space + repDegree + Constants.crlf + Constants.crlf;

        return msg.getBytes();
    }


    @Override
    public String toString() {
        return super.toString() + Constants.space + chunkNum + Constants.space + repDegree + "\n";
    }
}
