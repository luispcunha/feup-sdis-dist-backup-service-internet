package worker.delete;

import message.delete.DeletedMessage;
import peer.Peer;
import worker.Worker;

public class DeletedWorker extends Worker {
    private Peer peer;
    private DeletedMessage message;

    public DeletedWorker(Peer peer, DeletedMessage message) {
        this.peer = peer;
        this.message = message;
    }

    /**
     * Processes a DELETED message. This message is part of our delete enhancement and
     * works just as the STORED for the backup protocol. That said, this method will
     * remove a peer from the confirmed stores and check if the file has already been
     * completely deleted.
     */
    @Override
    public void work() {
        this.peer.getState().removeUndeletedChunk(message.getFileId(), message.getChunkNum());
        this.peer.closeChannel(super.getSocket());
    }
}
