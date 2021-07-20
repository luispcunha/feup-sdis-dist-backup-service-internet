package message.shift;

import message.ProtocolMessage;
import util.Constants;
import util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ShiftChunkMessage extends ProtocolMessage {
    private int chunkNum;
    private int repDegree;
    private byte[] body;

    public ShiftChunkMessage(String senderId, String fileId, String chunkNo, String repDegree, byte[] body) {
        super("SHIFT_CHUNK", senderId, fileId);
        this.chunkNum = Integer.parseInt(chunkNo);
        this.body = body;
        this.repDegree = Integer.parseInt(repDegree);
    }

    public ShiftChunkMessage(long senderId, String fileId, int chunkNo, int repDegree, byte[] body) {
        super("SHIFT_CHUNK", senderId, fileId);
        this.chunkNum = chunkNo;
        this.body = body;
        this.repDegree = repDegree;
    }

    public int getChunkNum() {
        return chunkNum;
    }

    public int getRepDegree() {
        return repDegree;
    }

    public byte[] getBody() {
        return body;
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
        return super.toString() + " " + chunkNum + " " + repDegree + "\n          Body Size = " + body.length + " bytes \n";
    }
}
