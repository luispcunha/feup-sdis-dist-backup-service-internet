package util;

public class Constants {
    public final static String space = " ";
    public final static String separator = ":";
    public final static String crlf = "\r\n";
    public final static int MAX_CHUNCK_BYTES = 64000;
    public final static int MAX_HEADER_BYTES = 100;
    public final static int MAX_CHORD_BYTES = 500;
    public final static int hashSeqLength = 64;
    public final static int MAX_TRIES = 5;
    public final static int DFT_SLEEP_TIME = 1000;
    public final static int BASE_POOL_SIZE = 50;
    public static final int TIMEOUT = 10000;


    public static final int PERIODICITY = 1000;
    public static final int LOG_CHORD_STATE_INTERVAL =  5000;
    public static final int TIME_OFFSET = 300;


    public static final boolean DEBUG_CHORD = false;
    public static final boolean CHORD_STATE = true;
    public static final boolean LOG_CHORD_MSGS = false;
    public static final boolean LOG_PEER_MSGS = true;

    public final static String storage_folder = "storage";
    public final static String backup_folder = "/backup";
    public final static String restore_folder = "/restore";
    public final static String encoding_file = "/encoding";
    public final static String recovery_file = "/storage_recovery";

    public final static String chunk_file = "chunk_";

    // ERRORS
    public final static int CHUNK_NOT_FOUND = -1;
    public final static int FILE_NOT_FOUND = -2;
    public final static int CHUNK_TOO_LARGE = -3;
    public final static int NUM_TRIES_EXCEEDED = -4;

    public final static String NO_FILEID = "NO_FILEID";
    public final static String FILE_DOESNT_EXIST = "FILE_DOESNT_EXIST";

    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    // Bold
    public static final String BLACK_BOLD = "\033[1;30m";  // BLACK
    public static final String RED_BOLD = "\033[1;31m";    // RED
    public static final String GREEN_BOLD = "\033[1;32m";  // GREEN
    public static final String YELLOW_BOLD = "\033[1;33m"; // YELLOW
    public static final String BLUE_BOLD = "\033[1;34m";   // BLUE
    public static final String PURPLE_BOLD = "\033[1;35m"; // PURPLE
    public static final String CYAN_BOLD = "\033[1;36m";   // CYAN
    public static final String WHITE_BOLD = "\033[1;37m";  // WHITE

    // Bold High Intensity
    public static final String BLACK_BOLD_BRIGHT = "\033[1;90m"; // BLACK
    public static final String RED_BOLD_BRIGHT = "\033[1;91m";   // RED
    public static final String GREEN_BOLD_BRIGHT = "\033[1;92m"; // GREEN
    public static final String YELLOW_BOLD_BRIGHT = "\033[1;93m";// YELLOW
    public static final String BLUE_BOLD_BRIGHT = "\033[1;94m";  // BLUE
    public static final String PURPLE_BOLD_BRIGHT = "\033[1;95m";// PURPLE
    public static final String CYAN_BOLD_BRIGHT = "\033[1;96m";  // CYAN
    public static final String WHITE_BOLD_BRIGHT = "\033[1;97m"; // WHITE
}
