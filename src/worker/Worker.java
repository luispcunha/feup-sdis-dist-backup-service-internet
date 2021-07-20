package worker;

import java.net.Socket;

public abstract class Worker {
    private Socket socket;

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public abstract void work();
}
