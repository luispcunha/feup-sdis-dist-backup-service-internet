package worker.successor;

import chord.ChordNode;
import message.successor.GetSuccMessage;
import message.successor.SendSuccMessage;
import worker.Worker;

public class GetSuccessorWorker  extends Worker {

    ChordNode chordNode;
    GetSuccMessage message;

    public GetSuccessorWorker(ChordNode node, GetSuccMessage message){
        this.chordNode = node;
        this.message = message;
    }

    @Override
    public void work() {
        SendSuccMessage msg = new SendSuccMessage(this.chordNode.getID(), String.valueOf(message.getKey()), chordNode.lookup(message.getKey()));

        chordNode.sendReply(msg, this.getSocket());
    }
}
