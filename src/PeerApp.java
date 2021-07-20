import peer.Peer;
import peer.PeerInterface;
import util.Log;

import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class PeerApp {
    public static void main(String[] args)  {
        if (args.length != 3 && args.length != 5) {
            System.out.println(
                    "Usage: java Peer <rmi_ap> <peer_port> <chord_port> [<chord_node_addr> <chord_node_port>]");
            System.exit(-1);
        }

        String remoteObjName = args[0];
        int peerPort = Integer.parseInt(args[1]);
        int chordPort = Integer.parseInt(args[2]);

        Peer peer = null;

        if (args.length == 5) {
            String chordNodeAddr = args[3];
            int chordNodePort = Integer.parseInt(args[4]);
            try {
                peer = new Peer(chordPort, peerPort, chordNodeAddr, chordNodePort);
            } catch (UnknownHostException e) {
                Log.logError("Unable to join Chord Ring");
                return;
            }
        } else {
            try {
                peer = new Peer(chordPort, peerPort);
            } catch (UnknownHostException e) {
                Log.logError("Unable to create Chord Ring");
                return;
            }
        }

        PeerInterface peerStub;
        Registry registry;

        try {
            peerStub = (PeerInterface) UnicastRemoteObject.exportObject(peer, 0);
            registry = LocateRegistry.getRegistry();
            registry.rebind(remoteObjName, peerStub);
        } catch (RemoteException e) {
            Log.logError("Unable to setup RMI");
            System.exit(-1);
        }
        
        Log.log("RMI access point is " + remoteObjName);
    }
}