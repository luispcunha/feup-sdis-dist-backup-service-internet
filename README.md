# Distributed Backup Service

Distributed backup service for the internet. The system is decentralized, using the Chord protocol so that peers can find where files are stored. Replication was used to ensured fault tolerance.

## Instructions

We have compiled and run our project successfully using Java 11 and Java 13.

1. Compilation

In order to compile the project we have created a script named 'compile.sh', located in the root of the project.
To run the script just open a terminal on the project's root folder and run './compile.sh' (given that the script has the necessary permissions).


2. Run

After compilation a build folder is created, which contains all the .class files.
To be able to test the project rmiregistry must be running in the build folder, by executing the command "rmiregistry &".
To facilitate running and testing the peers and the TestApp we have created a few scripts:

    - run_test_app.sh
        - runs the TestApp
        - usage: ./run_test_app.sh <rmi_ap> <operation> <opnd_1> <opnd_2>

    - run_peer.sh
        - runs a Peer
        - usage: ./run_peer.sh <rmi_ap> <peer_port> <chord_port> (creates a chord ring)
                 ./run_peer.sh <rmi_ap> <peer_port> <chord_port> <chord_node_addr> <chord_node_port> (join an existing chord ring)

    - cleanup.sh
        - deletes all the peers file systems
        - usage: ./cleanup.sh (to delete all peers' file systems)


Alternatively the Peers and TestApp may be run in the following way, respectively:
    - java PeerApp <rmi_ap> <peer_port> <chord_port> [<chord_node_addr> <chord_node_port>]
    - java TestApp <rmi_ap> <operation> <opnd_1> <opnd_2>


Bernardo Manuel Esteves dos Santos - up201706534
Carlos Jorge Direito Albuquerque - up201706735
Luís Pedro Pereira Lopes Mascarenhas Cunha - up201706736
Tito Alexandre Trindade Griné - up201706732
