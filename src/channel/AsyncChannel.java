package channel;

import message.Message;
import processor.Processor;
import util.Constants;
import util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Arrays;
import java.util.concurrent.*;

public class AsyncChannel implements CommunicationChannel {
    private AsynchronousServerSocketChannel socket;
    private Processor processor;
    private boolean closed;
    private ExecutorService pool;

    public AsyncChannel(int port) {
        this.pool = Executors.newCachedThreadPool();

        try {
            socket = AsynchronousServerSocketChannel.open(AsynchronousChannelGroup.withCachedThreadPool(this.pool, 1));
            socket.bind(new InetSocketAddress(port));
        } catch (IOException e) {
            Log.logError("Unable to connect to socket with port " + port + ".");
        }

        this.closed = false;
    }

    private Socket openChannel(InetAddress address, int port){
        try {
            AsynchronousSocketChannel socket = AsynchronousSocketChannel.open();
            Future<Void> future = socket.connect(new InetSocketAddress(address, port));
            future.get();

            return new BufferSocket(socket);
        } catch (Exception e) {
            Log.logError("Unable to establish connection to server with address " + address + " and port " + port + "using Async Socket.");
        }

        return null;
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
        ((BufferSocket) socket).sendMessage(msg.getBytes());
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

        BufferSocket socket = (BufferSocket) openChannel(address, port);

        if(socket == null)
            return null;

        this.sendMessage(msg, socket);
        Future<Message> messageFuture = this.pool.submit(new AsyncSocket(socket));

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
        ((BufferSocket) socket).sendMessage(msg.getBytes());
        ((BufferSocket) socket).clearBuffer();
        ((BufferSocket) socket).close();
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
        while (!this.closed) {
            socket.accept(this.processor, new CompletionHandler<>() {
                @Override
                public void completed(AsynchronousSocketChannel channel, Processor attachment) {
                    try {
                        if(channel.isOpen()){
                            socket.accept(attachment, this);
                        } else
                            return;

                        byte[] msg = new byte[Constants.MAX_CHORD_BYTES];
                        int offset = 0;
                        ByteBuffer buffer = null;

                        try {
                            while (true) {
                                ByteBuffer aux = ByteBuffer.allocate(Constants.MAX_CHORD_BYTES);
                                Future<Integer> reader = channel.read(aux);

                                reader.get();

                                aux.flip();

                                int off = aux.remaining();

                                if (off <= 0) {
                                    channel.shutdownInput();
                                    buffer = aux;
                                    break;
                                }

                                byte[] byteBuffer = new byte[Constants.MAX_CHORD_BYTES];

                                aux.get(byteBuffer, 0, off);
                                System.arraycopy(byteBuffer, 0, msg, offset, off);
                                offset += off;

                            }

                            attachment.processMessage(Arrays.copyOfRange(msg, 0, offset), new BufferSocket(channel, buffer)).run();

                        } catch (InterruptedException | ExecutionException e) {
                            Log.logError(e.getMessage());
                        }

                    } catch (Exception e) {
                        Log.logError(e.getMessage());
                    }

                }

                @Override
                public void failed(Throwable exc, Processor attachment) {
                    Log.logError("Failed to accept connection from asynchronous serve socket.");
                }
            });

            try {
                System.in.read();
            } catch (IOException e) {
                Log.logError(e.getMessage());
            }
        }

        try {
            this.socket.close();
        } catch (IOException e) {
            Log.logError("Failed to close asynchronous server socket.");
        }
    }
}