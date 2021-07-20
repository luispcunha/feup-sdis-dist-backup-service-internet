package protocol;

import chord.NodeInfo;
import filesystem.ChunkInfo;
import message.Message;
import message.shift.ShiftMessage;
import message.shift.ShiftRightMessage;
import peer.Peer;
import util.Log;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.List;

public class ShiftRightStoredInitiator implements Runnable {
    private Peer peer;
    private NodeInfo predecessor;

    public ShiftRightStoredInitiator(Peer peer, NodeInfo predecessor) {
        this.peer = peer;
        this.predecessor = predecessor;
    }

    @Override
    public void run() {
        List<ChunkInfo> chunks = peer.getState().getPredecessorChunks(predecessor.getId(), peer.getID());

        // The chunks list only holds the actual chunks that have to be shifted
        for(ChunkInfo chunkInfo : chunks) {
            try {
                peer.getFileSystem().loadChunk(chunkInfo.getFileID(), chunkInfo.getChunkNo(), new CompletionHandler<>() {
                    @Override
                    public void completed(Integer integer, ByteBuffer attachment) {
                        attachment.flip();
                        byte[] data = new byte[attachment.limit()];
                        attachment.get(data);
                        attachment.clear();

                        ShiftRightMessage predMessage = new ShiftRightMessage(peer.getID(), chunkInfo.getFileID(), chunkInfo.getChunkNo(), chunkInfo.getRepDegree(), data);

                        Message reply = peer.sendRequestToNode(predecessor, predMessage);

                        if(reply == null) return;

                        if (reply.getMessageType().equals("SHIFTED")) {
                            if(chunkInfo.getRepDegree() == 1) {
                                peer.getState().removeStoredChunk(chunkInfo.getFileID(), chunkInfo.getChunkNo(), chunkInfo.getSize());
                                peer.getFileSystem().deleteChunk(chunkInfo.getKey());
                            } else {
                                chunkInfo.decRepDegree();
                                ShiftMessage shiftMessage = new ShiftMessage(peer.getID(), chunkInfo.getFileID(), chunkInfo.getChunkNo(), chunkInfo.getRepDegree());
                                peer.redirectMessageSuccessor(shiftMessage);
                            }
                        }
                    }

                    @Override
                    public void failed(Throwable throwable, ByteBuffer byteBuffer) {
                        Log.logError("Failed loading chunk " + chunkInfo.getChunkNo() + " of file " + chunkInfo.getFileID());
                    }
                });
            } catch (Exception e) {
                Log.logError("Failed loading chunk " + chunkInfo.getChunkNo() + " of file " + chunkInfo.getFileID());
                continue;
            }
        }
    }
}
