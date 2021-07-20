package message.restore;

import message.ProtocolMessage;
import util.Constants;
import util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ChunkMessage extends ProtocolMessage {
    private int chunkNum;
    private byte[] body;

    public ChunkMessage(String senderId, String fileId, String chunkNo, byte[] body) {
        super("CHUNK", senderId, fileId);
        this.chunkNum = Integer.parseInt(chunkNo);
        this.body = body;
    }

    public ChunkMessage(long senderId, String fileId, int chunkNo, byte[] body) {
        super("CHUNK", senderId, fileId);
        this.chunkNum = chunkNo;
        this.body = body;
    }

    public int getChunkNum() {
        return chunkNum;
    }

    public byte[] getBody() {
        return body;
    }

    public byte[] getBytes() {
        String msg = getInfo() + Constants.space + chunkNum + Constants.crlf + Constants.crlf;
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
        return super.toString() + " " + chunkNum + "\n          Body Size = " + body.length + " bytes \n";
    }
}
