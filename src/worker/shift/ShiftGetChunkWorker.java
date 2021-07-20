package worker.shift;

import file.ChunkKey;
import filesystem.PeerState;
import message.Message;
import message.shift.ShiftChunkMessage;
import message.shift.ShiftGetChunkMessage;
import peer.Peer;
import util.Log;
import worker.Worker;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

public class ShiftGetChunkWorker extends Worker {
    private Peer peer;
    private ShiftGetChunkMessage message;
    private ChunkKey chunkKey;

    public ShiftGetChunkWorker(Peer peer, ShiftGetChunkMessage message) {
        this.peer = peer;
        this.message = message;
        this.chunkKey = new ChunkKey(message.getFileId(), message.getChunkNum());
    }

    /**
     * Processes a SHIFT_CHUNK message. This method retrieves the chunk from storage in case it exists.
     * After that, it will reply directly to the requester.
     */
    @Override
    public void work() {
        PeerState state = this.peer.getState();
        if(message.getSenderId() == peer.getID())
            return;

        if (!state.isStoredChunk(message.getFileId(), message.getChunkNum())) {
            Long redirectPeerKey = state.getRedirect(new ChunkKey(message.getFileId(), message.getChunkNum()));

            if (redirectPeerKey == null)
                return;

            Message reply = this.peer.sendRequest(redirectPeerKey, new ShiftGetChunkMessage(message, peer.getID()));
            this.peer.sendReply(reply, super.getSocket(), message.getSenderId());
        } else {
            Socket replySocket = super.getSocket();
            try {
                this.peer.getFileSystem().loadChunk(chunkKey.getFileID(), chunkKey.getNumber(), new CompletionHandler<>() {
                    @Override
                    public void completed(Integer integer, ByteBuffer attachment) {
                        attachment.flip();
                        byte[] data = new byte[attachment.limit()];
                        attachment.get(data);
                        attachment.clear();

                        int repDegree = peer.getState().getStoredChunkPerceivedRepDegree(chunkKey.getFileID(), chunkKey.getNumber());
                        Message reply = new ShiftChunkMessage(peer.getID(), chunkKey.getFileID(), chunkKey.getNumber(), repDegree, data);
                        peer.sendReply(reply, replySocket, message.getSenderId());
                    }

                    @Override
                    public void failed(Throwable throwable, ByteBuffer byteBuffer) {
                        Log.logError("Failed loading chunk " + chunkKey.getNumber() + " of file " + chunkKey.getFileID());
                    }
                });
            } catch (Exception e) {
                Log.logError("Failed loading chunk " + chunkKey.getNumber() + " of file " + chunkKey.getFileID());
            }
        }
    }
}
