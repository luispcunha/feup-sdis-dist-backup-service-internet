package message.backup;

import message.ProtocolMessage;
import util.Constants;
import util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class RedirectMessage extends ProtocolMessage {
    private int chunkNum;
    private int repDegree;
    private byte[] body;

    public RedirectMessage(String senderId, String fileId, String chunkNum, String repDegree, byte[] body) {
        super("REDIRECT", senderId, fileId);
        this.chunkNum = Integer.parseInt(chunkNum);
        this.repDegree = Integer.parseInt(repDegree);
        this.body = body;
    }

    public RedirectMessage(long senderId, PutChunkMessage putChunkMessage) {
        super("REDIRECT", senderId, putChunkMessage.getFileId());
        this.chunkNum = putChunkMessage.getChunkNum();
        this.repDegree = putChunkMessage.getRepDegree();
        this.body = putChunkMessage.getBody();
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
