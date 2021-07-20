package chord;

import util.Log;

import java.io.Serializable;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;

public class NodeInfo implements Serializable {
    private long id;
    private final InetAddress ipAddress;
    private final int chordPort;
    private final int peerPort;

    public NodeInfo(InetAddress ipAddress, int chordPort) {
        this.setNodeID(ipAddress, chordPort);
        this.ipAddress = ipAddress;
        this.chordPort = chordPort;
        this.peerPort = -1;
    }

    public NodeInfo(InetAddress ipAddress, int chordPort, int peerPort) {
        this.setNodeID(ipAddress, chordPort);
        this.ipAddress = ipAddress;
        this.chordPort = chordPort;
        this.peerPort = peerPort;
    }

    public NodeInfo(long id, InetAddress ipAddress, int chordPort, int peerPort) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.chordPort = chordPort;
        this.peerPort = peerPort;
    }

    public long getId() {
        return id;
    }

    public InetAddress getIpAddress() {
        return ipAddress;
    }

    public int getChordPort() {
        return chordPort;
    }

    public int getPeerPort() {
        return peerPort;
    }

    @Override
    public String toString() {
        return "NodeInfo - {id: " + id + ", ipAddress: " + ipAddress + ", chordPort: " + chordPort + ", peerPort: " + peerPort + '}';
    }

    private void setNodeID(InetAddress addr, int port){
        try{
            String ip = addr.getHostAddress() + port;
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(ip.getBytes("UTF-8"));

            ByteBuffer buffer = ByteBuffer.allocate(ChordNode.m);
            buffer.put(bytes, 0, ChordNode.m);
            buffer.flip();

            this.id = Math.abs(buffer.getLong()) % (long) Math.pow(2, ChordNode.m);
        } catch(Exception e){
            Log.logError(e.getMessage());
        }
    }
}
