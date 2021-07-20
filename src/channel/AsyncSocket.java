package channel;

import message.Message;
import message.MessageParser;
import util.Log;

import java.io.IOException;
import java.util.concurrent.Callable;

public class AsyncSocket implements Callable {

    BufferSocket socket;

    public AsyncSocket(BufferSocket socket){
        this.socket = socket;
    }

    @Override
    public Message call(){

        Message reply = null;

        try{
            byte[] msg = socket.getMessage();

            if(msg == null)
                return null;

            reply = MessageParser.parseMessage(msg);

        } catch(Exception e){
            Log.logError(e.getMessage());
        }

        socket.close();

        return reply;
    }

    public void close() throws IOException {
        this.socket.close();
    }
}
