package message;

import util.Constants;

public abstract class Message {
    private String messageType;
    private long senderId;

    protected Message(String messageType, String senderId) {
        this.messageType = messageType;
        this.senderId = Long.parseLong(senderId);
    }

    protected Message(String messageType, long senderId) {
        this.messageType = messageType;
        this.senderId = senderId;
    }

    protected String getInfo() {
        return messageType + Constants.space + senderId;
    }

    public String getMessageType() {
        return messageType;
    }

    public long getSenderId() {
        return senderId;
    }

    public abstract byte[] getBytes();

    @Override
    public String toString() {
        return this.messageType + Constants.space + this.senderId;
    }
}
