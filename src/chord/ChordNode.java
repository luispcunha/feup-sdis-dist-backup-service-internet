package chord;

import channel.AsyncChannel;
import channel.CommunicationChannel;
import message.Message;
import peer.MiddleLayerInterface;
import util.Constants;
import util.Log;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChordNode implements ChordInterface {
    private List<NodeInfo> fingerTable;
    private LinkedList<NodeInfo> successors;
    private NodeInfo predecessor;
    private NodeInfo thisNode;

    public static int m = 8;
    private static int r = 3;
    private int nextFinger = -1;
    private ChordProtocol protocol;
    private CommunicationChannel carrier;
    private ScheduledExecutorService scheduler;
    private MiddleLayerInterface middleLayer;

    private Thread communicationThread;

    private void updateSuccessors() {
        NodeInfo firstActiveNode = successors.peekFirst();
        NodeInfo previousSuccessor = successors.peekFirst();
        LinkedList<NodeInfo> newSuccessors;

        while (firstActiveNode != null && !protocol.ping(firstActiveNode)) {
            synchronized (successors) {
                successors.pollFirst();
                firstActiveNode = successors.peekFirst();
            }
        }

        if (firstActiveNode == null) {
            newSuccessors = new LinkedList<>();
            newSuccessors.add(thisNode);
        } else {
            newSuccessors = protocol.getSuccessorsList(firstActiveNode);

            if (newSuccessors == null) {
                Log.printError("List of new successors given was null, updateSuccessors() aborted.");
                return;
            }

            newSuccessors.addFirst(firstActiveNode);

            if (r < newSuccessors.size())
                newSuccessors.pollLast();
        }

        synchronized (successors) {
            successors = newSuccessors;
        }

        synchronized (fingerTable) {
            fingerTable.set(0, successors.get(0));
        }

        // Checking if there os a new successor
        if(successors.get(0).getId() != previousSuccessor.getId())
            middleLayer.newSuccessorCallback(successors.get(0));
    }


    private void startCommunication() {
        communicationThread = new Thread(this.carrier);
        communicationThread.start();
    }

    private void startPeriodicChecks() {
        this.scheduler = Executors.newScheduledThreadPool(4);

        if (Constants.DEBUG_CHORD) {
            this.scheduler.scheduleWithFixedDelay(this::stabilizeDebug, 0, Constants.PERIODICITY, TimeUnit.MILLISECONDS);
            this.scheduler.scheduleWithFixedDelay(this::fixFingersDebug, Constants.TIME_OFFSET, Constants.PERIODICITY, TimeUnit.MILLISECONDS);
            this.scheduler.scheduleWithFixedDelay(this::checkPredecessorDebug, 2 * Constants.TIME_OFFSET, Constants.PERIODICITY, TimeUnit.MILLISECONDS);
        } else  {
            this.scheduler.scheduleWithFixedDelay(this::stabilize, 0, Constants.PERIODICITY, TimeUnit.MILLISECONDS);
            this.scheduler.scheduleWithFixedDelay(this::fixFingers, Constants.TIME_OFFSET, Constants.PERIODICITY, TimeUnit.MILLISECONDS);
            this.scheduler.scheduleWithFixedDelay(this::checkPredecessor, 2 * Constants.TIME_OFFSET, Constants.PERIODICITY, TimeUnit.MILLISECONDS);
        }

        if (Constants.CHORD_STATE) {
            this.scheduler.scheduleWithFixedDelay(this::print, 1000, Constants.LOG_CHORD_STATE_INTERVAL, TimeUnit.MILLISECONDS);
        }
    }

    private void stabilizeDebug() {
        try {
            stabilize();
        } catch (Exception e) {
            Log.logError(e.getMessage());
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Log.logError(ex.getMessage());
            }
        }
    }

    private void fixFingersDebug() {
        try {
            fixFingers();
        } catch (Exception e) {
            Log.logError(e.getMessage());
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Log.logError(ex.getMessage());
            }
        }
    }

    private void checkPredecessorDebug() {
        try {
            checkPredecessor();
        } catch (Exception e) {
            Log.logError(e.getMessage());
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Log.logError(ex.getMessage());
            }
        }
    }

    private void endCommunication() {
        this.carrier.close();
    }

    public ChordNode(MiddleLayerInterface peer, int chordPort, int peerPort) throws UnknownHostException {
        this.thisNode = new NodeInfo(InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()), chordPort, peerPort);
        this.protocol = new ChordProtocol(this);
        this.carrier = new AsyncChannel(chordPort);
        this.carrier.setProcessor(new ChordProcessor(this));
        this.middleLayer = peer;

        this.fingerTable = new ArrayList<>(m);
        this.successors = new LinkedList<>();

        this.startCommunication();
    }

    public ChordNode(MiddleLayerInterface peer, long id, int chordPort, int peerPort) throws UnknownHostException {
        this.thisNode = new NodeInfo(InetAddress.getByName(InetAddress.getLocalHost().getHostAddress()), chordPort, peerPort);
        this.protocol = new ChordProtocol(this);
        this.carrier = new AsyncChannel(chordPort);
        this.carrier.setProcessor(new ChordProcessor(this));
        this.middleLayer = peer;

        this.fingerTable = new ArrayList<>(m);
        this.successors = new LinkedList<>();

        this.startCommunication();
    }

    public NodeInfo lookup(long id) {
        NodeInfo firstSuccessor = getSuccessor();

        if(firstSuccessor == null)
            return null;

        if(ChordInterface.intervalID(this.getID(), id, firstSuccessor.getId()) || id == firstSuccessor.getId())
            return firstSuccessor;

        NodeInfo closestNode = closestPrecedingNode(id);

        if (closestNode.getId() == thisNode.getId())
            return thisNode;

        return protocol.findSuccessor(closestNode, id);
    }

    public NodeInfo closestPrecedingNode(long id) {
        NodeInfo successor, finger;

        for (int i = m - 1; i >= 0; i--) {
            if (i < successors.size()) {
                synchronized (successors) {
                    successor = this.successors.get(i);
                    if (ChordInterface.intervalID(this.getID(), successor.getId(), id)) {
                        if (protocol.ping(successor))
                            return successor;
                    }
                }
            }

            if (i < fingerTable.size()) {
                synchronized (fingerTable) {
                    finger = this.fingerTable.get(i);

                    if (ChordInterface.intervalID(this.getID(), finger.getId(), id)) {
                        if (protocol.ping(finger))
                            return finger;
                    }
                }
            }
        }

        return thisNode;
    }

    public void create() {
        predecessor = null;
        fingerTable.add(thisNode);
        successors.add(thisNode);

        this.startPeriodicChecks();
    }

    public void join(NodeInfo node) throws UnknownHostException {
        int max_tries = 5;
        predecessor = null;
        NodeInfo successor = null;

        while(successor == null && max_tries > 0){
            successor = protocol.findSuccessor(node, this.getID());
            max_tries--;
        }

        if(successor == null)
            throw new UnknownHostException();

        fingerTable.add(successor);
        successors.add(successor);

        middleLayer.newSuccessorCallback(successor);

        this.startPeriodicChecks();
    }

    public void stabilize() {
        Log.printHighlight("Performing stabilize...");

        updateSuccessors();

        NodeInfo successor = getSuccessor();
        NodeInfo possibleSuccessor = protocol.findPredecessor(successor);

        if (possibleSuccessor == null) {
            Log.printError("Possible successor given was null in stabilize().");
        }

        if (possibleSuccessor != null && ChordInterface.intervalID(this.getID(), possibleSuccessor.getId(), successor.getId())) {
            synchronized (fingerTable) {
                fingerTable.set(0, possibleSuccessor);
            }

            synchronized (successors) {
                successors.set(0, possibleSuccessor);
            }

            successor = possibleSuccessor;
            middleLayer.newSuccessorCallback(successor);
        }

        protocol.notify(thisNode, successor);

        Log.printHighlight("Ending stabilize.");
    }

    public void notify(NodeInfo node) {
        if(predecessor == null || (ChordInterface.intervalID(predecessor.getId(), node.getId(), this.getID()))) {
            predecessor = node;
            middleLayer.newPredecessorCallback(node);
        }
    }

    public void fixFingers() {
        Log.printHighlight("Performing fixFingers...");

        nextFinger = (nextFinger + 1) % m;
        long key = (this.getID() + (long) Math.pow(2, nextFinger)) % (long) Math.pow(2, m);

        NodeInfo successor = this.lookup(key);

        if (successor == null) {
            Log.printError("Successor given was null, fixFingers() was aborted.");
            return;
        }

        synchronized (fingerTable) {
            if (fingerTable.size() <= nextFinger) {
                fingerTable.add(nextFinger, successor);
            } else {
                fingerTable.set(nextFinger, successor);
            }
        }

        Log.printHighlight("Ending fixFingers.");
    }

    public void checkPredecessor() {

        Log.printHighlight("Performing checkPredecessor...");

        if (predecessor != null && !protocol.ping(predecessor)) {
            predecessor = null;
        }

        Log.printHighlight("Ending checkPredecessor.");
    }

    public long getID() {
        return this.thisNode.getId();
    }

    public InetAddress getAddress() {
        return thisNode.getIpAddress();
    }

    public int getPort() {
        return thisNode.getChordPort();
    }

    public LinkedList<NodeInfo> getSuccessors() {
        return successors;
    }

    public NodeInfo getPredecessor() {
        return predecessor;
    }

    public NodeInfo getSuccessor() {
        NodeInfo successor;
        for(int i = 0; i < successors.size(); i++) {
            successor = successors.get(i);

            if(protocol.ping(successor))
                return successor;
        }

        return null;
    }

    public void sendMessage(Message message, NodeInfo receiverNode) {
        Log.logSentChord(message.toString() + "\n");
        this.carrier.sendMessage(message, receiverNode.getIpAddress(), receiverNode.getChordPort());
    }

    public Message sendRequest(Message message, NodeInfo receiverNode) {
        Message msg = this.carrier.sendRequest(message, receiverNode.getIpAddress(), receiverNode.getChordPort());
        Log.logSentChord(message.toString());

        if (msg != null)
            Log.logReceivedChord(msg.toString() + "\n");
        else
            Log.logReceivedChord("NULL\n");

        return msg;
    }

    public void sendReply(Message message, Socket socket) {
        Log.logSentChord(message.toString() + "\n");
        this.carrier.sendReply(message, socket);
    }

    public static void main(String[] args) throws UnknownHostException {
        //FIXME: to run this we now need a peer
        ChordNode node = new ChordNode(null, Long.parseLong(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
        Log.setPeerID(node.getID());

        if(args.length > 3)
            node.join(new NodeInfo(InetAddress.getByName(args[4]), Integer.parseInt(args[5])));
        else
            node.create();
    }

    public void print() {
        System.out.println("\n" + Constants.BLUE + " ============================== Chord State ============================== \n" + Constants.RESET);
        System.out.println(Constants.PURPLE_BOLD + " · Node: " + Constants.RESET + thisNode.toString() + "\n");
        System.out.println(Constants.PURPLE_BOLD + " · Predecessor Node: " + Constants.RESET + (predecessor != null ? predecessor.toString() : "null") + "\n");
        System.out.println(Constants.PURPLE_BOLD + " · Successors: " + Constants.RESET);
        for (NodeInfo successor : successors) {
            System.out.println("\t" + successor.toString());
        }
        System.out.println("");
        System.out.println(Constants.PURPLE_BOLD + " · Finger Table: " + Constants.RESET);
        for (NodeInfo finger : this.fingerTable) {
            System.out.println("\t" + finger.toString());
        }
        System.out.println("");
        System.out.println(Constants.BLUE + " ========================================================================= \n" + Constants.RESET + "\n");

    }
}
