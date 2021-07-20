package protocol;

import file.ChunkIDGenerator;
import message.delete.DeleteMessage;
import peer.Peer;
import util.Log;

import java.util.List;

public class DeleteInitiator implements Runnable {
    private Peer peer;
    private String fileId;
    private int numTries;
    private long rate;

    public DeleteInitiator(Peer peer, String fileID, int numTries, long rate) {
        this.peer = peer;
        this.fileId = fileID;
        this.numTries = numTries;
        this.rate = rate;
    }

    @Override
    public void run() {
        List<Integer> chunks = this.peer.getState().deleteBackupFile(this.fileId);

        for(int chunk : chunks) {
            peer.submitWorker(() -> {
                try {
                    long key = ChunkIDGenerator.generateID(this.fileId, chunk);
                    peer.sendMessage(key, new DeleteMessage(this.peer.getID(), this.fileId, chunk));
                } catch (Exception e) {
                    Log.logError(e.getMessage());
                }
            });
        }

        this.numTries--;
        if (this.numTries == 0)
            return;

        this.peer.scheduleTask(this, rate);
    }
}