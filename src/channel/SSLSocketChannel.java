package channel;

import message.Message;
import processor.Processor;
import util.Constants;
import util.Log;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class SSLSocketChannel implements CommunicationChannel {

    SSLServerSocketFactory serverSocketFactory;
    SSLSocketFactory socketFactory;

    private boolean closed;
    private ExecutorService pool;
    private int port;
    private Processor processor;

    private void setCredentials(){
        System.setProperty("javax.net.ssl.keyStore", "src/channel/security/server");
        System.setProperty("javax.net.ssl.keyStorePassword", "sdis2020");
        System.setProperty("javax.net.ssl.keyStore", "src/channel/security/client");
        System.setProperty("javax.net.ssl.keyStorePassword", "sdis2020");
        System.setProperty("javax.net.ssl.trustStore", "src/channel/security/truststore");
        System.setProperty("javax.net.ssl.trustStorePassword", "sdis2020");
    }

    public SSLSocketChannel(int port) {
        this.port = port;
        this.setCredentials();
        serverSocketFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        this.closed = false;
        this.pool = Executors.newCachedThreadPool();
    }

    private Socket openChannel(InetAddress address, int port){
        try {
            return socketFactory.createSocket(address, port);
        } catch (IOException e) {
            Log.logError("Unable to create SSL socket connected to address " + address.getHostAddress() + " and port " + port + ".");
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
        SSLServerSocket socket = null;

        try {
            socket = (SSLServerSocket) this.serverSocketFactory.createServerSocket(this.port);
            socket.setNeedClientAuth(true);
        }
        catch( IOException e) {
            Log.logError("Failed to create SSL server socket. Aborting.");
            return;
        }

        try {
            while(!closed){
                SSLSocket channel = (SSLSocket) socket.accept();

                this.pool.execute(() -> {
                    try {
                        DataInputStream inputStream = new DataInputStream(channel.getInputStream());
                        byte[] msg;

                        int length = inputStream.readInt();

                        if(length > 0) {
                            msg = new byte[length];
                            inputStream.readFully(msg, 0, length);
                            processor.processMessage(msg, channel).run();
                        }
                    } catch (IOException e) {
                        Log.logError(e.getMessage());
                    }
                });
            }
        } catch (IOException e) {
            Log.logError(e.getMessage());
        }
    }
}