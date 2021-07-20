package worker.restore;

import file.Chunk;
import file.ChunkKey;
import filesystem.PeerState;
import message.Message;
import message.restore.ChunkMessage;
import message.restore.GetChunkMessage;
import peer.Peer;
import util.Log;
import worker.Worker;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

public class GetChunkWorker extends Worker {
    private Peer peer;
    private GetChunkMessage message;
    private Chunk chunk;
    private ChunkKey chunkKey;

    public GetChunkWorker(Peer peer, GetChunkMessage message) {
        this.peer = peer;
        this.message = message;
        this.chunkKey = new ChunkKey(message.getFileId(), message.getChunkNum());
    }

    /**
     * Processes a GETCHUNK message. This method retrieves the chunk from storage in case it exists.
     * After that, it will schedule a new thread to call the run() method after a random delay.
     */
    @Override
    public void work() {
        PeerState state = this.peer.getState();

        if(message.getSenderId() == peer.getID())
            return;

        if (! state.isStoredChunk(message.getFileId(), message.getChunkNum())) {
            // check for an entry on the redirect table
            Long redirectPeerKey = state.getRedirect(new ChunkKey(message.getFileId(), message.getChunkNum()));
            if (redirectPeerKey == null)
                return;

            this.peer.sendMessage(redirectPeerKey, message);

            return;
        }

        Socket socket = super.getSocket();

        try {
            this.peer.getFileSystem().loadChunk(chunkKey.getFileID(), chunkKey.getNumber(), new CompletionHandler<>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    attachment.flip();
                    byte[] data = new byte[attachment.limit()];
                    attachment.get(data);
                    attachment.clear();

                    Message reply = new ChunkMessage(peer.getID(), chunkKey.getFileID(), chunkKey.getNumber(), data);
                    peer.sendMessage(message.getSenderId(), reply);
                    peer.closeChannel(socket);
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    Log.logError("Failed loading chunk " + chunkKey.getNumber() + " of file " + chunkKey.getFileID());
                }
            });
        } catch (IOException e) {
            Log.logError("Failed loading chunk " + chunkKey.getNumber() + " of file " + chunkKey.getFileID());
            return;
        }
    }
}
