package worker.shift;

import chord.ChordInterface;
import file.ChunkIDGenerator;
import file.ChunkKey;
import filesystem.PeerState;
import message.shift.ShiftMessage;
import peer.Peer;
import util.Log;
import worker.Worker;

public class ShiftWorker extends Worker {

    private Peer peer;
    private ShiftMessage message;

    private PeerState state;
    private String fileId;
    private int chunkNum;

    public ShiftWorker(Peer peer, ShiftMessage message) {
        this.peer = peer;
        this.message = message;

        this.state = peer.getState();
        this.fileId = message.getFileId();
        this.chunkNum = message.getChunkNum();
    }

    @Override
    public void work() {
        long chunkID = 0;

        try {
            chunkID = ChunkIDGenerator.generateID(this.fileId, this.chunkNum);
        } catch (Exception e) {
            Log.logError("Failed creating chunk ID");
        }

        if (chunkID == this.peer.getID() || ChordInterface.intervalID(chunkID, this.peer.getID(), message.getSenderId())) {
            return;
        }

        if (this.state.isStoredChunk(this.fileId, this.chunkNum)) {
            int previousRD = this.state.getStoredChunkPerceivedRepDegree(this.fileId, this.chunkNum);
            int newRD = message.getRepDegree() - 1;

            if(newRD <= 0) {
                try {
                    this.peer.getFileSystem().deleteChunk(new ChunkKey(this.fileId, this.chunkNum));
                    this.state.removeStoredChunk(this.fileId, this.chunkNum);
                } catch (Exception e) {
                    Log.logError("Unable to remove chunk.");
                }

                if (previousRD > 1) {
                    this.peer.redirectMessageSuccessor(new ShiftMessage(this.peer.getID(), this.fileId, this.chunkNum, message.getRepDegree()));
                }
            } else {
                this.state.setStoredChunkRepDegree(this.fileId, this.chunkNum, newRD);
                this.peer.redirectMessageSuccessor(new ShiftMessage(this.peer.getID(), this.fileId, this.chunkNum, newRD));
            }
        }
        else {
            Long redirectTo = this.state.getRedirect(new ChunkKey(this.fileId, this.chunkNum));
            if (redirectTo == null)
                return;

//            this.peer.sendMessage(redirectTo, new ShiftMessage(this.peer.getID(), this.fileId, this.chunkNum, this.message.getRepDegree()));
            this.peer.sendMessage(redirectTo, new ShiftMessage(this.peer.getID(), this.fileId, this.chunkNum, this.message.getRepDegree()));

            if (this.message.getRepDegree() <= 1)
                this.state.removeRedirect(new ChunkKey(this.fileId, this.chunkNum));
        }
    }

}
