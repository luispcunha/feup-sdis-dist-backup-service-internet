package message.notify;

import message.Message;
import util.Constants;
import util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NotifyMessage extends Message {
    InetAddress nodeAddress;
    int nodePort;
    int peerPort;

    public NotifyMessage(String id, String address, String port, String peerPort){
        super("NOTIFY", id);

        try {
            this.nodeAddress = InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            System.err.println(e);
            Log.logError(e.getMessage());
        }
        this.nodePort = Integer.parseInt(port);
        this.peerPort = Integer.parseInt(peerPort);
    }

    public NotifyMessage(long id, InetAddress address, int port, int peerPort){
        super("NOTIFY", id);

        this.nodeAddress = address;
        this.nodePort = port;
        this.peerPort = peerPort;
    }

    public InetAddress getNodeAddress() {
        return nodeAddress;
    }

    public int getNodePort() {
        return nodePort;
    }

    public int getPeerPort() {
        return this.peerPort;
    }

    @Override
    public byte[] getBytes() {
        String msg = getInfo() + Constants.space + nodeAddress.getHostAddress() + Constants.space + nodePort + Constants.space + peerPort + Constants.crlf + Constants.crlf;

        return msg.getBytes();
    }

    @Override
    public String toString() {
        return super.toString() + Constants.space + nodeAddress.getHostAddress() + Constants.space + nodePort + Constants.space + peerPort;
    }
}
