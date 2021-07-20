package protocol;

import file.ChunkKey;
import message.Message;
import message.restore.ChunkMessage;
import message.restore.GetChunkMessage;
import peer.Peer;
import util.Log;
import worker.restore.ChunkWorker;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

public class ChunkRestoreInitiator implements Runnable {

    private Peer peer;
    private ChunkKey chunkKey;

    public ChunkRestoreInitiator(Peer peer, ChunkKey chunkKey) {
        this.peer = peer;
        this.chunkKey = chunkKey;
    }

    @Override
    public void run() {
        Message message = new GetChunkMessage(this.peer.getID(), this.chunkKey.getFileID(), this.chunkKey.getNumber());

        if(peer.getState().isStoredChunk(chunkKey.getFileID(),chunkKey.getNumber())) {
            try {
                peer.getFileSystem().loadChunk(chunkKey.getFileID(), chunkKey.getNumber(), new CompletionHandler<>() {
                    @Override
                    public void completed(Integer integer, ByteBuffer attachment) {
                        attachment.flip();
                        byte[] data = new byte[attachment.limit()];
                        attachment.get(data);
                        attachment.clear();

                        peer.submitWorker(new ChunkWorker(peer, new ChunkMessage(peer.getID(), chunkKey.getFileID(), chunkKey.getNumber(), data)));
                    }

                    @Override
                    public void failed(Throwable throwable, ByteBuffer byteBuffer) {
                        Log.logError("Failed loading chunk " + chunkKey.getNumber() + " of file " + chunkKey.getFileID());
                    }
                });
            } catch (IOException e) {
                Log.logError("Failed loading chunk " + chunkKey.getNumber() + " of file " + chunkKey.getFileID());
            }
        } else {
            try {
                this.peer.sendMessage(chunkKey.getChordKey(), message);
            } catch (Exception e) {
                Log.logError(e.getMessage());
            }
        }
    }
}