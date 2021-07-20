package worker.backup;

import file.ChunkKey;
import filesystem.PeerState;
import message.backup.StoredMessage;
import peer.Peer;
import worker.Worker;

public class StoredWorker extends Worker {
    private StoredMessage message;
    private Peer peer;

    public StoredWorker(Peer peer, StoredMessage message) {
        this.message = message;
        this.peer = peer;
    }

    /**
     * Processes a STORED message: simply adds the peer id to the confirmed stores of the chunk number
     * present in the message header.
     */
    @Override
    public void work() {
        ChunkKey chunkKey = new ChunkKey(message.getFileId(), message.getChunkNum());
        long peerID = message.getSenderId();
        PeerState state = this.peer.getState();

        // initiator peer
        if (state.isBackupFile(chunkKey.getFileID()))
            state.addFileInfo(chunkKey.getFileID(), chunkKey.getNumber(), peerID);
        else // other peers
            state.addPeerBackingUpStoredChunk(chunkKey.getFileID(), chunkKey.getNumber(), peerID);

        this.peer.closeChannel(super.getSocket());
    }
}
