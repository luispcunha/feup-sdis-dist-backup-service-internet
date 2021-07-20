package message.delete;

import message.ProtocolMessage;
import util.Constants;

public class DeletedMessage extends ProtocolMessage {

    private int chunkNum;

    public DeletedMessage(String senderId, String fileId, String chunkNum){
        super("DELETED", senderId, fileId);
        this.chunkNum = Integer.parseInt(chunkNum);
    }

    public DeletedMessage(long senderId, String fileId, int chunkNum){
        super("DELETED", senderId, fileId);
        this.chunkNum = chunkNum;
    }

    public int getChunkNum() {
        return this.chunkNum;
    }

    @Override
    public byte[] getBytes() {
        String msg = getInfo() + Constants.space + chunkNum + Constants.crlf + Constants.crlf;

        return msg.getBytes();
    }

    @Override
    public String toString() {
        return super.toString() + " " + chunkNum + "\n";
    }
}
