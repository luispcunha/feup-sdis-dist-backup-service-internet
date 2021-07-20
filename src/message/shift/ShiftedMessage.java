package message.shift;

import message.ProtocolMessage;
import util.Constants;

public class ShiftedMessage extends ProtocolMessage {
    private int chunkNum;
    private int repDegree;

    public ShiftedMessage(String senderId, String fileId, String chunkNum, String repDegree) {
        super("SHIFTED", senderId, fileId);
        this.chunkNum = Integer.parseInt(chunkNum);
        this.repDegree = Integer.parseInt(repDegree);
    }

    public ShiftedMessage(long senderId, String fileId, int chunkNum, int repDegree) {
        super("SHIFTED", senderId, fileId);
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
