package channel;

import message.Message;
import processor.Processor;

import java.net.InetAddress;
import java.net.Socket;

public interface CommunicationChannel extends Runnable {
    void closeChannel(Socket socket);
    void sendMessage(Message msg, InetAddress address, int port);
    Message sendRequest(Message msg, InetAddress address, int port);
    void sendReply(Message msg, Socket socket);
    void setProcessor(Processor processor);
    void close();
}
