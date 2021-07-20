package message.backup;

import message.ProtocolMessage;
import util.Constants;
import util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PutChunkMessage extends ProtocolMessage {
    private int chunkNum;
    private int repDegree;
    private byte[] body;

    public PutChunkMessage(String senderId, String fileId, String chunkNum, String repDegree, byte[] body) {
        super("PUTCHUNK", senderId, fileId);
        this.chunkNum = Integer.parseInt(chunkNum);
        this.repDegree = Integer.parseInt(repDegree);
        this.body = body;
    }

    public PutChunkMessage(long senderId, String fileId, int chunkNum, int repDegree, byte[] body) {
        super("PUTCHUNK", senderId, fileId);
        this.chunkNum = chunkNum;
        this.repDegree = repDegree;
        this.body = body;
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
        }

        return msgBytes;
    }

    @Override
    public String toString() {
        return super.toString() + " " + chunkNum + " " + repDegree + "\n          Body Size = " + body.length + " bytes \n";
    }
}
