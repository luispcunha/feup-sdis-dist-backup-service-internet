package worker.successor;

import chord.ChordNode;
import message.successor.GetListMessage;
import message.successor.SendListMessage;
import worker.Worker;

public class GetListWorker  extends Worker {

    ChordNode chordNode;
    GetListMessage message;

    public GetListWorker(ChordNode node, GetListMessage message){
        this.chordNode = node;
        this.message = message;
    }

    @Override
    public void work() {
        SendListMessage msg = new SendListMessage(this.chordNode.getID(), this.chordNode.getSuccessors());


        chordNode.sendReply(msg, this.getSocket());
    }
}
