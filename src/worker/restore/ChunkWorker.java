package worker.restore;

import file.Chunk;
import message.restore.ChunkMessage;
import peer.Peer;
import protocol.ChunkRestoreSynchronizer;
import util.Log;
import worker.Worker;

import java.util.List;

public class ChunkWorker extends Worker implements Runnable {
    private Peer peer;
    private ChunkMessage message;

    public ChunkWorker(Peer peer, ChunkMessage message) {
        this.peer = peer;
        this.message = message;
    }

    /**
     * Processes a CHUNK message. This runs the vanilla version of the protocol,
     * which means that it will only save the chunk in a restored chunks vector
     * if this was the initiator peer of the restore protocol. Otherwise, the chunk
     * is simply signalized as already restored by the peer that sent this message.
     */
    @Override
    public void work() {
        Chunk chunk = new Chunk(message.getFileId(), message.getChunkNum(), message.getBody());
        ChunkRestoreSynchronizer chunkRestoreSync = this.peer.getChunkRestoreSync();

        chunkRestoreSync.chunkReceived(chunk);

        if (chunkRestoreSync.isRestoringFile(chunk.getFileID())) {
            List<Chunk> chunks = chunkRestoreSync.allChunksReceived(chunk.getFileID());

            if (chunks == null)
                return;

            Log.log("Received all chunks of file " + chunk.getFileID());
            this.peer.getFileSystem().restoreFile(chunks);
        }

        this.peer.closeChannel(super.getSocket());
    }

    @Override
    public void run() {
        work();
    }
}
