package worker.notify;

import chord.ChordNode;
import chord.NodeInfo;
import message.notify.NotifyMessage;
import worker.Worker;

public class NotifyWorker  extends Worker {

    ChordNode chordNode;
    NotifyMessage message;

    public NotifyWorker(ChordNode node, NotifyMessage message){
        this.chordNode = node;
        this.message = message;
    }

    @Override
    public void work() {
        chordNode.notify(new NodeInfo(message.getSenderId(), message.getNodeAddress(), message.getNodePort(), message.getPeerPort()));
    }
}
