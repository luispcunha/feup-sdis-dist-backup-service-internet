package message.successor;

import message.Message;
import util.Constants;

public class GetSuccMessage extends Message {
    private long nodeKey;

    public GetSuccMessage(String id, String key){
        super("GET_SUCCESSOR", id);
        this.nodeKey = Long.parseLong(key);
    }

    public GetSuccMessage(long id, long key){
        super("GET_SUCCESSOR", id);
        this.nodeKey = key;
    }

    public long getKey() {
        return nodeKey;
    }

    @Override
    public byte[] getBytes() {
        String msg = getInfo() + Constants.space + nodeKey + Constants.crlf + Constants.crlf;

        return msg.getBytes();
    }

    @Override
    public String toString() {
        return super.toString() + Constants.space + nodeKey + '\n';
    }
}
