package message;

import util.Constants;

public abstract class ProtocolMessage extends Message {
    private String fileId;

    protected ProtocolMessage(String messageType, String senderId, String fileId) {
        super(messageType, senderId);
        this.fileId = fileId;
    }

    protected ProtocolMessage(String messageType, long senderId, String fileId) {
        super(messageType, senderId);
        this.fileId = fileId;
    }

    protected String getInfo() {
        return super.getInfo() + Constants.space + fileId;
    }

    public String getFileId() {
        return fileId;
    }

    @Override
    public String toString() {
        return super.toString() + " " + this.fileId;
    }
}