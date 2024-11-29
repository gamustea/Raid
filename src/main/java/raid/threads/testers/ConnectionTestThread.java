package raid.threads.testers;

import static raid.misc.Util.*;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class ConnectionTestThread extends Thread{
    private final int port;
    private final String host;
    private final CyclicBarrier barrier;
    private boolean connectionAvailable;

    public ConnectionTestThread(int port, String host, CyclicBarrier barrier) {
        this.port = port;
        this.host = host;
        this.barrier = barrier;
    }

    @Override
    public void run() {
        Socket s = null;
        try {
            s = new Socket(host, port);
            this.connectionAvailable = true;
            barrier.await();
        }
        catch (IOException | InterruptedException | BrokenBarrierException e) {
            this.connectionAvailable = false;
        }
        closeResource(s);
    }

    public boolean isConnectionAvailable() {
        return connectionAvailable;
    }
}
