package peer;

import channel.CommunicationChannel;
import channel.SSLSocketChannel;
import chord.ChordInterface;
import chord.ChordNode;
import chord.NodeInfo;
import file.*;
import filesystem.ChunkInfo;
import filesystem.FileSystem;
import filesystem.PeerState;
import message.Message;
import processor.PeerProcessor;
import protocol.*;
import util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Peer implements PeerInterface, MiddleLayerInterface {
    private ChordInterface chord;

    private FileSystem fileSystem;
    private PeerState state;

    private ExecutorService workers;
    private ScheduledExecutorService scheduler;

    // objects used to synchronize between threads working on the same protocol
    private ChunkRestoreSynchronizer chunkRestoreSync;
    // private SpaceReclaimSynchronizer spaceReclaimSync;

    private CommunicationChannel channel;

    // true if it's first time this peer is launched
    private boolean firstTime;

    private final int SCHEDULER_POOL_SIZE = 500;
    private final long SAVE_STATE_INTERVAL_MS = 2000;

    public Peer(int chordPort, int peerPort) throws UnknownHostException {
        this.chord = new ChordNode(this, chordPort, peerPort);

        this.channel = new SSLSocketChannel(peerPort);
        this.channel.setProcessor(new PeerProcessor(this));

        this.fileSystem = new FileSystem(this);
        this.firstTime = ! this.loadState();

        this.chunkRestoreSync = new ChunkRestoreSynchronizer();

        this.workers = Executors.newCachedThreadPool();
        this.scheduler = Executors.newScheduledThreadPool(SCHEDULER_POOL_SIZE);
        this.createChordRing();

        (new Thread(this.channel)).start();

        // schedule task that saves peer state to disk
        this.writeStateToDisk();

        Log.setPeerID(this.chord.getID());
    }

    public Peer(int chordPort, int peerPort, String chordNodeAddr, int chordNodePort) throws UnknownHostException {
        this.chord = new ChordNode(this, chordPort, peerPort);

        this.channel = new SSLSocketChannel(peerPort);
        this.channel.setProcessor(new PeerProcessor(this));

        this.fileSystem = new FileSystem(this);
        this.firstTime = ! this.loadState();

        this.chunkRestoreSync = new ChunkRestoreSynchronizer();

        this.workers = Executors.newCachedThreadPool();
        this.scheduler = Executors.newScheduledThreadPool(SCHEDULER_POOL_SIZE);
        this.joinChordRing(chordNodeAddr, chordNodePort);

        (new Thread(this.channel)).start();

        // schedule task that saves peer state to disk
        this.writeStateToDisk();

        Log.setPeerID(this.chord.getID());
    }

    public boolean loadState() {
        PeerState state = this.fileSystem.loadState();

        if (state == null) {
            this.state = new PeerState();
            return false;
        } else {
            this.state = state;
            state.savedToDisk();
            return true;
        }
    }

    /*  Chord related methods  */

    public void joinChordRing(String chordNodeAddr, int chordNodePort) throws UnknownHostException {
        this.chord.join(new NodeInfo(InetAddress.getByName(chordNodeAddr), chordNodePort));
    }

    public void createChordRing() {
        this.chord.create();
    }

    @Override
    public void newPredecessorCallback(NodeInfo predecessor) {
        this.workers.submit(new ShiftRightStoredInitiator(this, predecessor));
        this.workers.submit(new ShiftRightRedirectedInitiator(this, predecessor));
    }

    @Override
    public void newSuccessorCallback(NodeInfo successor) {
        this.workers.submit(new ShiftLeftStoredInitiator(this, successor));
        this.workers.submit(new ShiftLeftRedirectInitiator(this, successor));
    }

    /*  Comms related methods  */
    public Message sendRequestToNode(NodeInfo node, Message message) {
        Log.logSentPeer(message.toString() + " TO " + node.getId());

        Message reply = this.channel.sendRequest(message, node.getIpAddress(), node.getPeerPort());

        if (reply != null)
            Log.logReceivedPeer(reply.toString() + "\n");
        else
            Log.logReceivedPeer("NULL\n");

        return reply;
    }

    public Message sendRequest(long key, Message message) {
        NodeInfo node = this.chord.lookup(key);
        System.out.println("Looking up " + key + ", found " + node.getId());

        return sendRequestToNode(node, message);
    }

    public Message redirectRequestSuccessor(Message message) {
        NodeInfo successor = this.chord.getSuccessor();

        return sendRequestToNode(successor, message);
    }

    public void sendMessage(long key, Message message) {
        NodeInfo node = this.chord.lookup(key);
        System.out.println("Looking up " + key + ", found " + node.getId());

        this.channel.sendMessage(message, node.getIpAddress(), node.getPeerPort());
        Log.logSentPeer(message.toString() + " TO " + node.getId());
    }

    public NodeInfo redirectMessageSuccessor(Message message) {
        NodeInfo successor = this.chord.getSuccessor();

        this.channel.sendMessage(message, successor.getIpAddress(), successor.getPeerPort());

        Log.logSentPeer(message.toString() + " TO " + successor.getId());
        return successor;
    }

    public void sendReply(Message message, Socket socket, long id) {
        this.channel.sendReply(message, socket);
        Log.logSentPeer(message.toString() + " TO " + id);
    }

    public void closeChannel(Socket socket) {
        this.channel.closeChannel(socket);
    }

    public void writeStateToDisk() {
        this.scheduler.scheduleAtFixedRate(() -> fileSystem.storeState(state), 0, SAVE_STATE_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public void submitWorker(Runnable task) {
        this.workers.submit(task);
    }

    public void scheduleTask(Runnable task, long delay) {
        this.scheduler.schedule(task, delay, TimeUnit.MILLISECONDS);
    }

    public FileSystem getFileSystem() {
        return this.fileSystem;
    }

    public PeerState getState() {
        return this.state;
    }

    public long getID() { return chord.getID(); }

    public ChunkRestoreSynchronizer getChunkRestoreSync() {
        return this.chunkRestoreSync;
    }

    @Override
    public int backup(String path, int replicationDegree) throws RemoteException {

        try {
            FileHandler file = new FileHandler(path);

            if (! this.state.insertFileInfo(file.getAbsolutePath(), file.getID(), replicationDegree)) {
                Log.logError("File " + file.getID() + " already backed up");
                return -1;
            };

            file.loadChunks(new CompletionHandler<>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    attachment.flip();
                    byte[] data = new byte[attachment.limit()];
                    attachment.get(data);
                    attachment.clear();

                    boolean requiresChunk0Len = (data.length % Chunk.MAX_SIZE) == 0;
                    int numWholeChunks = data.length / Chunk.MAX_SIZE;

                    for (int i = 0; i < numWholeChunks; i++) {
                        Chunk chunk = new Chunk(file.getID(), i, Arrays.copyOfRange(data, i * Chunk.MAX_SIZE, (i + 1) * Chunk.MAX_SIZE));
                        workers.submit(new ChunkBackupInitiator(Peer.this, chunk, replicationDegree));
                    }

                    Chunk chunk;
                    if (requiresChunk0Len) {
                        chunk = new Chunk(file.getID(), numWholeChunks, new byte[0]);
                    } else {
                        chunk = new Chunk(file.getID(), numWholeChunks, Arrays.copyOfRange(data, numWholeChunks * Chunk.MAX_SIZE, data.length));
                    }

                    workers.submit(new ChunkBackupInitiator(Peer.this, chunk, replicationDegree));
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    Log.logError("Failed loading file to backup");
                }
            });
        } catch (IOException e) {
            Log.logError("Failed opening file");
            return -1;
        } catch (FileSizeException e) {
            Log.logError(e.toString());
            return -1;
        } catch (NoSuchAlgorithmException e) {
            Log.logError("Failed to generate file ID");
            return -1;
        }

        return 0;
    }

    @Override
    public int restore(String path) throws RemoteException {

        String fileID;
        try {
            fileID = FileIDGenerator.generateID(Paths.get(path));
        } catch (NoSuchAlgorithmException | IOException e) {
            Log.logError("Failed generating file ID");
            return -1;
        }

        long numChunks = 0;
        try {
            numChunks = Files.size(Paths.get(path)) / Chunk.MAX_SIZE + 1;
        } catch (IOException e) {
            Log.logError("Error computing size of file");
            return -1;
        }

        this.chunkRestoreSync.restoreFile(fileID);

        for (int i = 0; i < numChunks; i++) {
            this.workers.submit(new ChunkRestoreInitiator(this, new ChunkKey(fileID, i)));
        }

        return 0;
    }

    @Override
    public int delete(String path) throws RemoteException {

        String fileID;
        try {
            fileID = FileIDGenerator.generateID(Paths.get(path));
        } catch (NoSuchAlgorithmException | IOException e) {
            Log.logError("Failed generating file ID");
            return -1;
        }

        if (! state.isBackupFile(fileID)) {
            Log.logError("This peer didn't request backup of file " + fileID);
            return -1;
        }

        this.workers.submit(new DeleteInitiator(this, fileID,3, 1000));

        return 0;
    }

    @Override
    public int reclaim(int space) throws RemoteException {
        List<ChunkInfo> chunkKeys = this.state.reclaim(space);

        System.out.println("Chunks to delete: " + chunkKeys);

        for (ChunkInfo info : chunkKeys)
            this.workers.submit(new SpaceReclaimInitiator(this, new ChunkKey(info.getFileID(), info.getChunkNo()), info.getRepDegree()));

        //for (ChunkInfo info : chunkKeys)
        //    this.fileSystem.deleteChunk(new ChunkKey(info.getFileID(), info.getChunkNo()));

        return 0;
    }

    @Override
    public String state() throws RemoteException {
        return this.state.toString();
    }
}