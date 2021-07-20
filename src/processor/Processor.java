package processor;

import java.net.Socket;

public interface Processor {
    Runnable processMessage(byte[] msg, Socket channel);
}
