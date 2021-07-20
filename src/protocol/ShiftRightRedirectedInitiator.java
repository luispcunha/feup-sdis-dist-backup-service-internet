package protocol;

import chord.NodeInfo;
import file.ChunkKey;
import message.Message;
import message.shift.*;
import peer.Peer;

import java.util.Map;

public class ShiftRightRedirectedInitiator implements Runnable {
    private Peer peer;
    private NodeInfo predecessor;

    public ShiftRightRedirectedInitiator(Peer peer, NodeInfo predecessor) {
        this.peer = peer;
        this.predecessor = predecessor;
    }

    @Override
    public void run() {
        Map<ChunkKey, Long> redirects = peer.getState().getPredecessorRedirectPeers(predecessor.getId(), peer.getID());

        // The chunks list only holds the actual chunks that have to be shifted
        for(Map.Entry<ChunkKey, Long> entry : redirects.entrySet()) {
            ChunkKey chunkKey = entry.getKey();
            Long redirect = entry.getValue();

            ShiftGetChunkMessage chunkRequest = new ShiftGetChunkMessage(peer.getID(), chunkKey.getFileID(), chunkKey.getNumber());

            ShiftChunkMessage chunkReply = (ShiftChunkMessage) peer.sendRequest(redirect, chunkRequest);

            if(chunkReply == null) continue;

            ShiftRightMessage predMessage = new ShiftRightMessage(peer.getID(), chunkKey.getFileID(), chunkKey.getNumber(), chunkReply.getRepDegree(), chunkReply.getBody());

            Message reply = this.peer.sendRequestToNode(predecessor, predMessage);

            if(reply == null) continue;

            if (reply.getMessageType().equals("SHIFTED")) {
                ShiftedMessage shiftedMessage = (ShiftedMessage) reply;

                if(shiftedMessage.getRepDegree() == 1) {
                    peer.getState().removeRedirect(chunkKey);
                }

                ShiftMessage shiftMessage = new ShiftMessage(peer.getID(), chunkKey.getFileID(), chunkKey.getNumber(), shiftedMessage.getRepDegree());
                peer.redirectMessageSuccessor(shiftMessage);
            }
        }
    }
}
