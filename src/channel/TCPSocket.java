package channel;

import message.Message;
import message.MessageParser;
import util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Callable;

public class TCPSocket implements Callable {

    private Socket socket;

    public TCPSocket(Socket socket){
        this.socket = socket;
    }

    @Override
    public Message call() {
        try {
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            byte[] msg;

            int length = inputStream.readInt();
            int offset = 0;

            while(offset < length) {
                msg = new byte[length];
                offset += inputStream.read(msg, offset, length);
                Message reply = MessageParser.parseMessage(msg);
                return reply;
            }

            return null;
        } catch (IOException e) {
            Log.logError(e.getMessage());
        }

        return null;
    }
}
