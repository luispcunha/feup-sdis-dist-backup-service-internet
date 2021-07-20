package message.ping;

import message.Message;
import util.Constants;

public class PongMessage extends Message {
    public PongMessage(String id){
        super("PONG", id);
    }

    public PongMessage(long id){
        super("PONG", id);
    }

    @Override
    public byte[] getBytes() {
        String msg = getInfo() + Constants.crlf + Constants.crlf;

        return msg.getBytes();
    }
}

