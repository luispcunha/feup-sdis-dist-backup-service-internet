package worker.reclaim;

import file.Chunk;
import file.ChunkKey;
import message.reclaim.MovedMessage;
import message.reclaim.RemovedMessage;
import peer.Peer;
import util.Log;
import worker.Worker;

import java.io.IOException;

public class RemovedWorker extends Worker {
    private Peer peer;
    private RemovedMessage message;
    private ChunkKey chunkKey;

    public RemovedWorker(Peer peer, RemovedMessage message) {
        this.peer = peer;
        this.message = message;
        this.chunkKey = new ChunkKey(message.getFileId(), message.getChunkNum());
    }

    @Override
    public void work() {

        if (this.peer.getID() == this.message.getSenderId())
            return;

        boolean backup = this.peer.getState().isBackupFile(message.getFileId());
        boolean stored = this.peer.getState().isStoredChunk(message.getFileId(), message.getChunkNum());
        boolean space = this.peer.getState().getAvailableSpace() < message.getBody().length;

        if (stored) {
            this.peer.getState().setStoredChunkRepDegree(chunkKey.getFileID(), chunkKey.getNumber(), message.getRepDegree());

            if(message.getRepDegree() > 1)
                this.peer.redirectRequestSuccessor(new RemovedMessage(message.getSenderId(), chunkKey.getFileID(), chunkKey.getNumber(), message.getRepDegree() - 1, message.getBody()));
        } else if (backup || space) {
            MovedMessage received = (MovedMessage) this.peer.redirectRequestSuccessor(this.message);

            if (received == null)
                return;

            this.peer.getState().addRedirect(new ChunkKey(received.getFileId(), received.getChunkNum()), received.getSenderId());
        } else {
            try {
                this.peer.getState().addStoredChunkInfo(message.getFileId(), message.getRepDegree(), message.getChunkNum(), message.getBody().length);
                this.peer.getFileSystem().storeChunk(new Chunk(message.getFileId(), message.getChunkNum(), message.getBody()));
            } catch (IOException e) {
                Log.logError("Unable to store chunk.");
                return;
            }
        }

        MovedMessage answer = new MovedMessage(this.peer.getID(), message.getFileId(), message.getChunkNum());
        this.peer.sendReply(answer, super.getSocket(), message.getSenderId());
    }
}
