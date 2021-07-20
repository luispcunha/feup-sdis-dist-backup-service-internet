package worker.predecessor;

import chord.ChordNode;
import message.predecessor.GetPredMessage;
import message.predecessor.SendPredMessage;
import worker.Worker;

public class GetPredecessorWorker  extends Worker {

    ChordNode chordNode;
    GetPredMessage message;

    public GetPredecessorWorker(ChordNode node, GetPredMessage message){
        this.chordNode = node;
        this.message = message;
    }

    @Override
    public void work() {
        SendPredMessage msg = new SendPredMessage(this.chordNode.getID(), this.chordNode.getPredecessor());

        chordNode.sendReply(msg, this.getSocket());
    }
}