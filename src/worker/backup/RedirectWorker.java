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

public class RedirectWorker extends Worker {
    private Peer peer;
    private RedirectMessage message;
    private Chunk chunk;
    private PeerState state;

    public RedirectWorker(Peer peer, RedirectMessage message) {
        this.peer = peer;
        this.message = message;
        this.chunk = new Chunk(message.getFileId(), message.getChunkNum(), message.getBody());
        this.state = peer.getState();
    }

    /**
     * This method processes a REDIRECT message: similarly to the PUTCHUNK message, it stores tries to store the chunk
     * if all the conditions are met. If not, it redirects the message to the successor.
     */
    @Override
    public void work() {
        if(this.peer.getID() == message.getSenderId()) { //The redirect message completed a circle loop
            return;
        }

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
            Message reply = this.peer.redirectRequestSuccessor(message);

            if (reply == null) {
                return;
            }

            state.addRedirect(chunk.getKey(), reply.getSenderId());
        }

        Message reply = new StoredMessage(this.peer.getID(), message.getFileId(), chunk.getKey().getNumber());
        this.peer.sendReply(reply, super.getSocket(), message.getSenderId());
    }

    private boolean canBackupChunk() {
        return ! (state.getAvailableSpace() < chunk.getSize() || state.isBackupFile(chunk.getFileID()) || state.isStoredChunk(chunk.getFileID(), chunk.getNumber()));
    }
}
