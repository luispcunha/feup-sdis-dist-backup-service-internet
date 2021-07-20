package message.ping;

import message.Message;
import util.Constants;

public class PingMessage extends Message {
    public PingMessage(String id){
        super("PING", id);
    }

    public PingMessage(long id){
        super("PING", id);
    }

    @Override
    public byte[] getBytes() {
        String msg = getInfo() + Constants.crlf + Constants.crlf;

        return msg.getBytes();
    }
}
