package worker.shift;

import chord.ChordInterface;
import chord.NodeInfo;
import file.Chunk;
import file.ChunkIDGenerator;
import file.ChunkKey;
import filesystem.PeerState;
import message.shift.ShiftLeftMessage;
import message.shift.ShiftMessage;
import peer.Peer;
import util.Log;
import worker.Worker;

import java.io.IOException;

public class ShiftLeftWorker extends Worker {

    private Peer peer;
    private ShiftLeftMessage message;

    private PeerState state;
    private String fileId;
    private int chunkNum;

    public ShiftLeftWorker(Peer peer, ShiftLeftMessage message) {
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

        if (message.getSenderId() == this.peer.getID() || chunkID == this.peer.getID() ||
                ChordInterface.intervalID(chunkID, this.peer.getID(), message.getSenderId())) {
            return;
        }

        if (this.state.isStoredChunk(this.fileId, this.chunkNum)) {
            int previousRD = this.state.getStoredChunkPerceivedRepDegree(this.fileId, this.chunkNum);

            //previousRD > message.getRepDegree() &&
//            if(previousRD == message.getRepDegree()) {
//                return;
//            }

            this.state.setStoredChunkRepDegree(this.fileId, this.chunkNum, message.getRepDegree());

            if (message.getRepDegree() > 1) {
                this.peer.redirectMessageSuccessor(new ShiftLeftMessage(message.getSenderId(), this.fileId, this.chunkNum, message.getRepDegree() - 1, message.getBody()));
            }
            else {
                this.peer.redirectMessageSuccessor(new ShiftMessage(this.peer.getID(), this.fileId, this.chunkNum, previousRD - 1));
            }
        }
        else {
            if (this.state.isBackupFile(this.fileId) || this.state.getAvailableSpace() < message.getBody().length) {
                NodeInfo successor = this.peer.redirectMessageSuccessor(message);
                if (this.state.getRedirect(new ChunkKey(this.fileId, this.chunkNum)) == null)
                    this.state.addRedirect(new ChunkKey(this.fileId, this.chunkNum), successor.getId());
            } else {
                Chunk chunk = new Chunk(message.getFileId(), message.getChunkNum(), message.getBody());
                try {
                    this.peer.getFileSystem().storeChunk(chunk);
                    this.state.addStoredChunkInfo(chunk.getFileID(), message.getRepDegree(), chunk.getNumber(), chunk.getSize());
                } catch (IOException e) {
                    Log.logError("Failed storing chunk. FileId= " + message.getFileId() + "; ChunkNo= " + message.getChunkNum());
                }

                if (message.getRepDegree() > 1) {
                    this.peer.redirectMessageSuccessor(new ShiftLeftMessage(message.getSenderId(), this.fileId, this.chunkNum, message.getRepDegree() - 1, message.getBody()));
                }
                else {
                    this.peer.redirectMessageSuccessor(new ShiftMessage(this.peer.getID(), this.fileId, this.chunkNum, message.getRepDegree()));
                }
            }
        }
    }
}
