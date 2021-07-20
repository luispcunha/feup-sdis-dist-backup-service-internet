package message.predecessor;

import message.Message;
import util.Constants;

public class GetPredMessage extends Message {
    public GetPredMessage(String id){
        super("GET_PREDECESSOR", id);
    }

    public GetPredMessage(long id){
        super("GET_PREDECESSOR", id);
    }

    @Override
    public byte[] getBytes() {
        String msg = getInfo() + Constants.crlf + Constants.crlf;

        return msg.getBytes();
    }
}
