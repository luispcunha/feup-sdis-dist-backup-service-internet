package worker.ping;

import chord.ChordNode;
import message.ping.PingMessage;
import message.ping.PongMessage;
import worker.Worker;

public class PingWorker extends Worker {

    ChordNode chordNode;
    PingMessage message;

    public PingWorker(ChordNode node, PingMessage message){
        this.chordNode = node;
        this.message = message;
    }

    @Override
    public void work() {
        PongMessage msg = new PongMessage(this.chordNode.getID());

        chordNode.sendReply(msg, this.getSocket());
    }
}
