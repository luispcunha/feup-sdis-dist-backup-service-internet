package worker.shift;

import file.Chunk;
import file.ChunkKey;
import filesystem.PeerState;
import message.Message;
import message.shift.NotShiftedMessage;
import message.shift.ShiftRightMessage;
import message.shift.ShiftedMessage;
import peer.Peer;
import util.Log;
import worker.Worker;

public class ShiftRightWorker extends Worker {

    private Peer peer;
    private ShiftRightMessage message;

    private PeerState state;
    private String fileId;
    private int chunkNum;


    public ShiftRightWorker(Peer peer, ShiftRightMessage message) {
        this.peer = peer;
        this.message = message;

        this.state = peer.getState();
        this.fileId = message.getFileId();
        this.chunkNum = message.getChunkNum();
    }

    @Override
    public void work() {
        Message answer;

        if (!this.state.isStoredChunk(this.fileId, this.chunkNum)) {
            if (this.state.isBackupFile(this.fileId) || this.state.getAvailableSpace() < message.getBody().length) {
                this.state.addRedirect(new ChunkKey(this.fileId, this.chunkNum), this.message.getSenderId());
                answer = new NotShiftedMessage(this.peer.getID(), this.fileId, this.chunkNum, this.message.getRepDegree());
            }
            else {
                try {
                    this.peer.getFileSystem().storeChunk(new Chunk(this.fileId, this.chunkNum, this.message.getBody()));
                    this.state.addStoredChunkInfo(this.fileId, this.message.getRepDegree(), this.chunkNum, this.message.getBody().length);
                    // TODO: the replication degree sent is the same received
                    answer = new ShiftedMessage(this.peer.getID(), this.fileId, this.chunkNum, this.message.getRepDegree());
                } catch (Exception e) {
                    Log.logError("Unable to store chunk");
                    answer = new NotShiftedMessage(this.peer.getID(), this.fileId, this.chunkNum, this.message.getRepDegree());
                }
            }
        }
        else {
            int repDegree = this.state.getStoredChunkPerceivedRepDegree(this.fileId, this.chunkNum);
            answer = new NotShiftedMessage(this.peer.getID(), this.fileId, this.chunkNum, repDegree);
        }

        peer.sendReply(answer, super.getSocket(), this.peer.getID());
    }
}
