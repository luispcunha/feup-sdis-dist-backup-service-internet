package worker.backup;

import file.Chunk;
import filesystem.PeerState;
import message.Message;
import message.backup.PutChunkMessage;
import message.backup.RedirectMessage;
import message.backup.StoredMessage;
import peer.Peer;
import util.Log;
import worker.Worker;

import java.io.IOException;

public class PutChunkWorker extends Worker {
    private Peer peer;
    private PutChunkMessage message;
    private Chunk chunk;
    private PeerState state;

    public PutChunkWorker(Peer peer, PutChunkMessage message) {
        this.peer = peer;
        this.message = message;
        this.chunk = new Chunk(message.getFileId(), message.getChunkNum(), message.getBody());
        this.state = peer.getState();
    }

    /**
     * This method processes a PUTCHUNK message: it stores the chunk in storage if this was not
     * its initiator peer and if it is not already stored. In this last case, the chunk is signaled as
     * reclaimed, which means that it has been already sent by another peer as part of the reclaim protocol.
     * After that, this thread will schedule a new one to send the STORED message after a delay.
     */
    @Override
    public void work() {
        // state.removeUndeletedFile(message.getFileId());
        // this.peer.getSpaceReclaimSync().putChunkReceived(chunk.getKey());

        if (canBackupChunk()) {
            try {
                this.peer.getFileSystem().storeChunk(chunk);
                state.addStoredChunkInfo(chunk.getFileID(), message.getRepDegree(), chunk.getNumber(), chunk.getSize());
            } catch (IOException e) {
                Log.logError("Failed storing chunk");
                return;
            }

            if (message.getRepDegree() > 1) // There are still peers to redirect this message to, because desired RD hasn't been achieved
                this.peer.redirectMessageSuccessor(new PutChunkMessage(message.getSenderId(), message.getFileId(), message.getChunkNum(), message.getRepDegree() - 1, message.getBody()));
        } else {
            Message reply = this.peer.redirectRequestSuccessor(new RedirectMessage(this.peer.getID(), message));

            if (reply == null) {
                return;
            }

            state.addRedirect(chunk.getKey(), reply.getSenderId());
        }

        this.peer.closeChannel(super.getSocket());

        Message reply = new StoredMessage(this.peer.getID(), message.getFileId(), chunk.getKey().getNumber());
        this.peer.sendMessage(message.getSenderId(), reply);

    }

    private boolean canBackupChunk() {
        System.out.println("state.isBackupFile = " + state.isBackupFile(chunk.getFileID()) + " file id " + chunk.getFileID());
        return ! (state.getAvailableSpace() < chunk.getSize() || state.isBackupFile(chunk.getFileID()) || state.isStoredChunk(chunk.getFileID(), chunk.getNumber()));
    }
}
