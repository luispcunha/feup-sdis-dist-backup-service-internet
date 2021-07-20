package protocol;

import chord.NodeInfo;
import file.ChunkKey;
import message.shift.ShiftChunkMessage;
import message.shift.ShiftGetChunkMessage;
import message.shift.ShiftLeftMessage;
import peer.Peer;

import java.util.Map;
import java.util.Set;


public class ShiftLeftRedirectInitiator implements Runnable {

    private Peer peer;
    private NodeInfo successor;

    public ShiftLeftRedirectInitiator(Peer peer, NodeInfo successor) {
        this.peer = peer;
        this.successor = successor;
    }

    @Override
    public void run() {
        Set<Map.Entry<ChunkKey, Long>> redirects = peer.getState().getAllRedirects();

        for(Map.Entry<ChunkKey, Long> redirect : redirects) {
            ChunkKey chunkKey = redirect.getKey();
            long redirectID = redirect.getValue();

            ShiftGetChunkMessage chunkRequest = new ShiftGetChunkMessage(peer.getID(), chunkKey.getFileID(), chunkKey.getNumber());

            ShiftChunkMessage chunkReply = (ShiftChunkMessage) peer.sendRequest(redirectID, chunkRequest);

            if(chunkReply == null) {
                peer.getState().removeRedirect(chunkKey);
                continue;
            }

            peer.sendMessage(successor.getId(), new ShiftLeftMessage(peer.getID(), chunkKey.getFileID(), chunkKey.getNumber(), chunkReply.getRepDegree(), chunkReply.getBody()));
            peer.getState().removeRedirect(chunkKey);
            peer.getState().addRedirect(chunkKey, successor.getId());
        }
    }

}
