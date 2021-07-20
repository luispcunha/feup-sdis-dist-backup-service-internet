package channel;

import message.Message;
import processor.Processor;
import util.Constants;
import util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class TCPChannel implements CommunicationChannel {
    private ServerSocket socket;
    private Processor processor;
    private boolean closed;
    private ExecutorService pool;

    public TCPChannel(int port) {
        try {
            this.socket = new ServerSocket(port);
        } catch (IOException e) {
            Log.logError("Unable to connect to socket with port " + port + ".");
        }
        this.closed = false;
        this.pool = Executors.newCachedThreadPool();
    }

    private Socket openChannel(InetAddress address, int port){
        try {
            return new Socket(address, port);
        } catch (IOException e) {
            Log.logError("Unable to connect to socket with address " + address.getHostAddress() + " and port " + port + ".");
            return null;
        }
    }

    @Override
    public void closeChannel(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            Log.logError("Unable to close socket.");
        }
    }

    private void sendMessage(Message msg, Socket socket){
        byte[] bytes = msg.getBytes();

        try{
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeInt(bytes.length);
            outputStream.write(bytes);
            outputStream.flush();
        } catch (IOException e){
            Log.logError(e.getMessage());
        }
    }

    @Override
    public void sendMessage(Message msg, InetAddress address, int port) {
        Socket socket = openChannel(address, port);

        if (socket == null)
            return;

        sendMessage(msg, socket);
    }

    @Override
    public Message sendRequest(Message msg, InetAddress address, int port) {
        Socket socket = openChannel(address, port);

        if(socket == null)
            return null;

        this.sendMessage(msg, socket);

        Future<Message> messageFuture = this.pool.submit(new TCPSocket(socket));

        try {
            Message reply = messageFuture.get(Constants.TIMEOUT, TimeUnit.MILLISECONDS);

            return reply;
        } catch (Exception e) {
            Log.logError(e.getMessage());
        }
        return null;
    }

    @Override
    public void sendReply(Message msg, Socket socket) {
        this.sendMessage(msg, socket);
    }

    @Override
    public void setProcessor(Processor processor) {
        this.processor = processor;
    }

    @Override
    public void close() {
        this.closed = true;
    }
    
    @Override
    public void run() {
        try {
            while (!this.closed) {
                Socket channel = this.socket.accept();

                DataInputStream inputStream = new DataInputStream(channel.getInputStream());
                byte[] msg;

                int length = inputStream.readInt();

                if(length > 0) {
                    msg = new byte[length];
                    inputStream.readFully(msg, 0, length);
                    this.pool.submit(this.processor.processMessage(msg, channel));
                }
            }

            this.socket.close();
        } catch (IOException e) {
            Log.logError(e.getMessage());
        }
    }
}