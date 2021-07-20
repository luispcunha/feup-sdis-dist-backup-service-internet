package channel;

import util.Constants;
import util.Log;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class BufferSocket extends Socket {

    AsynchronousSocketChannel channel;
    ByteBuffer buffer;

    public BufferSocket(AsynchronousSocketChannel channel, ByteBuffer buffer){
        this.channel = channel;
        this.buffer = buffer;
    }

    public BufferSocket(AsynchronousSocketChannel channel){
        this.channel = channel;
        buffer = ByteBuffer.allocate(Constants.MAX_CHORD_BYTES);
    }

    public Future<Integer> write(){
        return channel.write(buffer);
    }

    public Future<Integer> write(byte[] msg){
        buffer = ByteBuffer.wrap(msg);

        return channel.write(buffer);
    }

    public void sendMessage(byte[] msg){

        Future<Integer> writer = this.write(msg);

        try {
            channel.shutdownOutput();
            writer.get();
        } catch (Exception e) {
            Log.logError("Error in attempting to write to ByteBuffer.");
        }

        buffer.flip();
    }

    public byte[] getMessage(){
         byte[] msg = new byte[Constants.MAX_CHORD_BYTES];
         int offset = 0;

         try{
            while(true){
                buffer = ByteBuffer.allocate(Constants.MAX_CHORD_BYTES);
                Future<Integer> reader = channel.read(buffer);
                reader.get();

                buffer.flip();
                int off = buffer.remaining();

                if(off <= 0){
                    channel.shutdownInput();
                    break;
                }

                byte[] byteBuffer = new byte[Constants.MAX_CHORD_BYTES];

                buffer.get(byteBuffer, 0, off);
                System.arraycopy(byteBuffer, 0, msg, offset, off);
                offset += off;
            }
         } catch (InterruptedException | ExecutionException | IOException e) {
             Log.logError(e.getMessage());
         }

        buffer.clear();

        return Arrays.copyOfRange(msg, 0, offset);
    }

    public void clearBuffer(){
        buffer.clear();
    }

    @Override
    public synchronized void close(){
        try {
            channel.close();
        } catch (IOException e) {
            Log.logError("Error in closing async socket channel.");
        }
    }
}
