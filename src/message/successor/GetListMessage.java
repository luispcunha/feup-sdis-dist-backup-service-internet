package message.successor;

import message.Message;
import util.Constants;

public class GetListMessage extends Message {
    public GetListMessage(String id){
        super("GET_LIST", id);
    }

    public GetListMessage(long id){
        super("GET_LIST", id);
    }

    @Override
    public byte[] getBytes() {
        String msg = getInfo() + Constants.crlf + Constants.crlf;

        return msg.getBytes();
    }
}
