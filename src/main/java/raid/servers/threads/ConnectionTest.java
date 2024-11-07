package raid.servers.threads;

import java.io.IOException;
import java.net.Socket;

public class ConnectionTest extends Thread{
    private final int PORT;
    private final String HOST;
    private boolean connectionAvailable;

    public ConnectionTest(int port, String host) {
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
        if (s != null) {
            try {
                s.close();
            }
            catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public boolean isConnectionAvailable() {
        return connectionAvailable;
    }
}
