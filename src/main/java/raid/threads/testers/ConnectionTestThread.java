package raid.threads.testers;

import raid.servers.Server;

import static raid.Util.*;

import java.io.IOException;
import java.net.Socket;

public class ConnectionTestThread extends Thread{
    private final int PORT;
    private final String HOST;
    private boolean connectionAvailable;

    public ConnectionTestThread(int port, String host) {
        this.PORT = port;
        this.HOST = host;
    }

    @Override
    public void run() {
        Socket s = null;
        while (super.isAlive()) {
            try {
                sleep(500);
                s = new Socket(HOST, PORT);
                this.connectionAvailable = true;
            }
            catch (InterruptedException | IOException e) {
                this.connectionAvailable = false;
            }
        }
        closeResource(s);
    }

    public boolean isConnectionAvailable() {
        return connectionAvailable;
    }
}
