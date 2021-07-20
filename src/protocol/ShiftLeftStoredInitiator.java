package protocol;

import chord.NodeInfo;
import filesystem.ChunkInfo;
import message.shift.ShiftLeftMessage;
import peer.Peer;
import util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

public class ShiftLeftStoredInitiator implements Runnable {

    private Peer peer;
    private NodeInfo successor;

    public ShiftLeftStoredInitiator(Peer peer, NodeInfo successor) {
        this.peer = peer;
        this.successor = successor;
    }

    @Override
    public void run() {
        for (ChunkInfo chunkInfo : this.peer.getState().getAllStoredChunks()) {
            if (chunkInfo.getRepDegree() < 2)
                continue;

            try {
                this.peer.getFileSystem().loadChunk(chunkInfo.getFileID(), chunkInfo.getChunkNo(), new CompletionHandler<>() {
                    @Override
                    public void completed(Integer integer, ByteBuffer attachment) {
                        attachment.flip();
                        byte[] data = new byte[attachment.limit()];
                        attachment.get(data);
                        attachment.clear();

                        peer.sendMessage(successor.getId(), new ShiftLeftMessage(peer.getID(), chunkInfo.getFileID(), chunkInfo.getChunkNo(), chunkInfo.getRepDegree() - 1, data));

                    }

                    @Override
                    public void failed(Throwable throwable, ByteBuffer byteBuffer) {
                        Log.logError("Failed loading chunk " + chunkInfo.getChunkNo() + " of file " + chunkInfo.getFileID());
                    }
                });
            } catch (IOException e ) {
                Log.logError("Failed loading chunk " + chunkInfo.getChunkNo() + " of file " + chunkInfo.getFileID());
            }
        }
    }

}
