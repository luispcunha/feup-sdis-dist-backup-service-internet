package worker.delete;

import file.ChunkKey;
import message.Message;
import message.delete.DeleteMessage;
import message.delete.DeletedMessage;
import peer.Peer;
import util.Log;
import worker.Worker;

import java.util.Random;

public class DeleteWorker extends Worker implements Runnable {
    private Peer peer;
    private DeleteMessage message;

    public DeleteWorker(Peer peer, DeleteMessage message) {
        this.peer = peer;
        this.message = message;
    }

    /**
     * Processes a DELETE message. Apart from the V1 behaviour, this method
     * will also schedule a new thread to reply with a DELETED message after a random delay.
     */
    @Override
    public void work() {
        int repDegree = this.peer.getState().getStoredChunkPerceivedRepDegree(message.getFileId(), message.getChunkNum());
        if (repDegree < 1) {
            // the chunk is not stored in this peer, as such a redirect may be present
            Long redirectTo = this.peer.getState().removeRedirect(new ChunkKey(message.getFileId(), message.getChunkNum()));

            if (redirectTo == null)
                return;

            this.peer.sendMessage(redirectTo, this.message);
        }
        else {
            if (repDegree > 1) {
                // this is not the last peer that is storing the chunk
                this.peer.redirectMessageSuccessor(new DeleteMessage(message.getSenderId(), message.getFileId(), message.getChunkNum()));
            }

            this.peer.getState().removeStoredChunk(message.getFileId(), message.getChunkNum());
            this.peer.getFileSystem().deleteChunk(new ChunkKey(message.getFileId(), message.getChunkNum()));

            int backoffTime = new Random().nextInt(500);
            this.peer.scheduleTask(this, backoffTime);
            Log.logBackoff(backoffTime, "before sending DELETED message for file " + message.getFileId());
        }

        this.peer.closeChannel(super.getSocket());
    }

    /**
     * Sends a DELETED message as reply to the received DELETE.
     */
    @Override
    public void run() {
        Message reply = new DeletedMessage(this.peer.getID(), message.getFileId(), message.getChunkNum());
        this.peer.sendMessage(message.getSenderId(), reply);
    }
}