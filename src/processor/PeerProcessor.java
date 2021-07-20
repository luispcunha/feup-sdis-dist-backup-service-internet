package processor;

import message.Message;
import message.MessageParser;
import message.backup.PutChunkMessage;
import message.backup.RedirectMessage;
import message.backup.StoredMessage;
import message.delete.DeleteMessage;
import message.delete.DeletedMessage;
import message.reclaim.RemovedMessage;
import message.restore.ChunkMessage;
import message.restore.GetChunkMessage;
import message.shift.ShiftGetChunkMessage;
import message.shift.ShiftLeftMessage;
import message.shift.ShiftMessage;
import message.shift.ShiftRightMessage;
import peer.Peer;
import util.Log;
import worker.Worker;
import worker.backup.PutChunkWorker;
import worker.backup.RedirectWorker;
import worker.backup.StoredWorker;
import worker.delete.DeleteWorker;
import worker.delete.DeletedWorker;
import worker.reclaim.RemovedWorker;
import worker.restore.ChunkWorker;
import worker.restore.GetChunkWorker;
import worker.shift.ShiftGetChunkWorker;
import worker.shift.ShiftLeftWorker;
import worker.shift.ShiftRightWorker;
import worker.shift.ShiftWorker;

import java.net.Socket;

public class PeerProcessor implements Runnable, Processor {
    private Peer peer;
    private byte[] rawMessage;
    private Socket socket;

    private PeerProcessor(Peer peer, byte[] message, Socket socket) {
        this.peer = peer;
        this.rawMessage = message;
        this.socket = socket;
    }

    public PeerProcessor(Peer peer) {
        this.peer = peer;
    }

    /**
     * Processes a message. The message attribute holds the parsed message.
     * According to the type of message, this method creates a worker to process it.
     * This is done by a WorkerFactory, which upon calling getWork() returns the specific
     * worker for this kind of message.
     * Note that messages from this peer are ignored.
     */
    @Override
    public void run() {
        Message message = MessageParser.parseMessage(rawMessage);
        Worker worker;

        if(message == null) return; //Ignoring messages that are not recognized

        switch (message.getMessageType()) {
            case "PUTCHUNK":
                worker = new PutChunkWorker(peer, (PutChunkMessage) message);
                break;

            case "REDIRECT":
                worker = new RedirectWorker(peer, (RedirectMessage) message);
                break;

            case "STORED":
                worker = new StoredWorker(peer, (StoredMessage) message);
                break;

            case "DELETE":
                worker = new DeleteWorker(peer, (DeleteMessage) message);
                break;

            case "DELETED":
                worker = new DeletedWorker(peer, (DeletedMessage) message);
                break;

            case "GETCHUNK":
                worker = new GetChunkWorker(peer, (GetChunkMessage) message);
                break;

            case "CHUNK":
                worker = new ChunkWorker(peer, (ChunkMessage) message);
                break;

            case "REMOVED":
                worker = new RemovedWorker(peer, (RemovedMessage) message);
                break;

            case "SHIFT":
                worker = new ShiftWorker(peer, (ShiftMessage) message);
                break;

            case "SHIFT_RIGHT":
                worker = new ShiftRightWorker(peer, (ShiftRightMessage) message);
                break;

            case "SHIFT_LEFT":
                worker = new ShiftLeftWorker(peer, (ShiftLeftMessage) message);
                break;

            case "SHIFT_GETCHUNK":
                worker = new ShiftGetChunkWorker(peer, (ShiftGetChunkMessage) message);
                break;

            default:
                return;
        }

        Log.logReceivedPeer(message.toString());

        worker.setSocket(socket);
        worker.work();
    }

    @Override
    public Runnable processMessage(byte[] msg, Socket socket) {
        return new PeerProcessor(peer, msg, socket);
    }
}
