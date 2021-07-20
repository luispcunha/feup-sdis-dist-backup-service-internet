package message.shift;

import message.ProtocolMessage;
import util.Constants;
import util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ShiftRightMessage extends ProtocolMessage {
    private int chunkNum;
    private int repDegree;
    private byte[] body;

    public ShiftRightMessage(String senderId, String fileId, String chunkNum, String repDegree, byte[] body) {
        super("SHIFT_RIGHT", senderId, fileId);
        this.chunkNum = Integer.parseInt(chunkNum);
        this.repDegree = Integer.parseInt(repDegree);
        this.body = body;
    }

    public ShiftRightMessage(long senderId, String fileId, int chunkNum, int repDegree, byte[] body) {
        super("SHIFT_RIGHT", senderId, fileId);
        this.chunkNum = chunkNum;
        this.repDegree = repDegree;
        this.body = body;
    }

    public int getChunkNum() {
        return chunkNum;
    }

    public int getRepDegree() {
        return this.repDegree;
    }

    public byte[] getBody() {
        return this.body;
    }

    public byte[] getBytes() {
        String msg = getInfo() + Constants.space + chunkNum + Constants.space + repDegree + Constants.crlf + Constants.crlf;
        byte[] msgBytes = new byte[0];

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            out.write(msg.getBytes());
            out.write(body);
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
        return super.toString() + Constants.space + chunkNum + Constants.space + repDegree + "\n          Body Size = " + body.length + " bytes \n";
    }
}
