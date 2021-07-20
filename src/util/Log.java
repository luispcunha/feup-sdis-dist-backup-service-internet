package util;

public class Log {
    static long peerID;

    public static void setPeerID(long id) {
        peerID = id;
    }

    public static void log(String msg) {
        System.out.println(" Peer " + peerID + " :: " + msg);
    }

    public static void logError(String msg) {
        log("ERROR :: " + msg);
    }

    public static void logBackoff(int time, String before) {
        log("Backing off for " + String.format("%03d", time) + "ms before " + before);
    }

    public static void logRepDegree(int desired, int perceived, int chunkNo) {
        log("Desired RD " + desired + " :: Perceived RD " + perceived + " :: Chunk " + chunkNo);
    }

    public static void logWaiting(long time, int chunkNo) {
        log("Waiting for " + time + "ms (chunk " + chunkNo + ")");
    }

    public static void logSent(String msg) {
        log(String.format("SENT :: %s", msg));
    }

    public static void logReceived(String msg) {
        log(String.format("RECEIVED :: %s", msg));
    }

    public static void logReceivedChord(String msg) {
        if (Constants.LOG_CHORD_MSGS)
            logReceived(msg);
    }

    public static void logSentChord(String msg) {
        if (Constants.LOG_CHORD_MSGS)
            logSent(msg);
    }

    public static void logReceivedPeer(String msg) {
        if (Constants.LOG_PEER_MSGS)
            logReceived(msg);
    }

    public static void logSentPeer(String msg) {
        if (Constants.LOG_PEER_MSGS)
            logSent(msg);
    }

    public static void printError(String msg){
        System.out.println(Constants.RED_BOLD_BRIGHT + msg + Constants.RESET + '\n');
    }

    public static void printHighlight(String msg) {
       if (Constants.LOG_CHORD_MSGS) System.out.println(" · " + Constants.YELLOW_BOLD_BRIGHT + msg + Constants.RESET + '\n');
    }
}