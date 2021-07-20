package protocol;

import file.ChunkKey;
import message.Message;
import message.reclaim.MovedMessage;
import message.reclaim.RemovedMessage;
import peer.Peer;
import util.Log;

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

public class SpaceReclaimInitiator implements Runnable {
    private Peer peer;
    private ChunkKey chunkKey;
    private int repDegree;

    public SpaceReclaimInitiator(Peer peer, ChunkKey chunkKey, int repDegree) {
        this.peer = peer;
        this.chunkKey = chunkKey;
        this.repDegree = repDegree;
    }

    @Override
    public void run() {
        try {
            System.out.println("Before loading chunk " + chunkKey.getNumber());
            this.peer.getFileSystem().loadChunk(chunkKey.getFileID(), chunkKey.getNumber(), new CompletionHandler<>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    attachment.flip();
                    byte[] data = new byte[attachment.limit()];
                    attachment.get(data);
                    attachment.clear();

                    Message message = new RemovedMessage(peer.getID(), chunkKey.getFileID(), chunkKey.getNumber(), repDegree, data);
                    MovedMessage reply = (MovedMessage) peer.redirectRequestSuccessor(message);

                    if (reply == null) {
                        return;
                    }

                    peer.getState().addRedirect(new ChunkKey(reply.getFileId(), reply.getChunkNum()), reply.getSenderId());
                    peer.getFileSystem().deleteChunk(chunkKey);
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    System.out.println("COMPLETED loading chunk " + chunkKey.getNumber());
                    Log.logError("Failed loading chunk " + chunkKey.getNumber() + " of file " + chunkKey.getFileID());
                    peer.getFileSystem().deleteChunk(chunkKey);
                }
            });
        } catch (FileNotFoundException e) {
            Log.logError(String.format("Chunk with chord key %d is not stored in this peer.", this.chunkKey.getChordKey()));
        } catch (Exception e) {
            Log.logError("Failed loading chunk " + chunkKey.getNumber() + " of file " + chunkKey.getFileID());
        }
    }
}