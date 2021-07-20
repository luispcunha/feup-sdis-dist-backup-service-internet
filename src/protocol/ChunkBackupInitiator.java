package protocol;

import file.Chunk;
import filesystem.PeerState;
import message.Message;
import message.backup.PutChunkMessage;
import peer.Peer;
import util.Constants;
import util.Log;

/**
 * Initiates the chunk backup subprotocol
 */
public class ChunkBackupInitiator implements Runnable {

    private Peer peer;
    private Chunk chunk;
    private int repDegree;

    /**
     * @param peer          peer for which the protocol is being executed
     * @param chunk         chunk to backup
     * @param repDegree     desired replication degree
     */
    public ChunkBackupInitiator(Peer peer, Chunk chunk, int repDegree) {
        this.peer = peer;
        this.chunk = chunk;
        this.repDegree = repDegree;
    }

    @Override
    public void run() {
        PeerState state = this.peer.getState();
        Message message = new PutChunkMessage(this.peer.getID(), this.chunk.getFileID(), this.chunk.getNumber(), this.repDegree, this.chunk.getContent());

        try {
            System.out.println("Chunk " + chunk.getChordKey());
            this.peer.sendMessage(chunk.getChordKey(), message);
        } catch (Exception e) {
            Log.logError(e.getMessage());
        }

        this.peer.scheduleTask(() -> {
            int currentRepDegree;

            // get current replication degree depending on whether the peer is the owner of the file or is
            // executing the protocol after receiving a removed message
            if (state.isBackupFile(chunk.getFileID()))
                currentRepDegree = state.getBackupChunkPerceivedRepDegree(chunk.getFileID(), chunk.getNumber());
            else
                currentRepDegree = state.getStoredChunkPerceivedRepDegree(chunk.getFileID(), chunk.getNumber());

            Log.logRepDegree(repDegree, currentRepDegree, chunk.getNumber());

            if (state.isBackupFile(chunk.getFileID())) {
                Log.log("Backed up chunk " + chunk.getNumber() + " of file " + chunk.getFileID() + " with RD " + currentRepDegree);
            }
        }, Constants.DFT_SLEEP_TIME);
    }
}