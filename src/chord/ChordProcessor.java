package chord;

import message.Message;
import message.MessageParser;
import message.notify.NotifyMessage;
import message.ping.PingMessage;
import message.predecessor.GetPredMessage;
import message.successor.GetListMessage;
import message.successor.GetSuccMessage;
import processor.Processor;
import util.Log;
import worker.Worker;
import worker.notify.NotifyWorker;
import worker.ping.PingWorker;
import worker.predecessor.GetPredecessorWorker;
import worker.successor.GetListWorker;
import worker.successor.GetSuccessorWorker;

import java.net.Socket;

public class ChordProcessor implements Runnable,Processor {

    ChordNode chordNode;
    private byte[] rawMessage;
    private Socket socket;

    private ChordProcessor(ChordNode node, byte[] message, Socket socket) {
        this.chordNode = node;
        this.rawMessage = message;
        this.socket = socket;
    }

    public ChordProcessor(ChordNode node){
        this.chordNode = node;
    }

    @Override
    public void run() {
        Message message = MessageParser.parseMessage(this.rawMessage);

        Worker worker;

        if(message == null) return; //Ignoring messages that are not recognized


        switch (message.getMessageType()) {
            case "PING":
                worker = new PingWorker(this.chordNode, (PingMessage) message);
                break;

            case "NOTIFY":
                worker = new NotifyWorker(this.chordNode, (NotifyMessage) message);
                break;

            case "GET_PREDECESSOR":
                worker = new GetPredecessorWorker(this.chordNode, (GetPredMessage) message);
                break;

            case "GET_SUCCESSOR":
                worker = new GetSuccessorWorker(this.chordNode, (GetSuccMessage) message);
                break;

            case "GET_LIST":
                worker = new GetListWorker(this.chordNode, (GetListMessage) message);
                break;

            default:
                return;
        }

        Log.logReceivedChord(message.toString() + "\n");

        worker.setSocket(this.socket);
        worker.work();
    }

    @Override
    public Runnable processMessage(byte[] msg, Socket channel) {
        return new ChordProcessor(this.chordNode, msg, channel);
    }
}
