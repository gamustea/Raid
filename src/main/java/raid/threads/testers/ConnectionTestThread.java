package raid.threads.testers;

import static raid.misc.Util.*;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;


/**
 * <p>Type of {@link Thread} that allows a process to remain idle if the connection
 * to a server it's trying to access is not up. In order to function, a {@link
 * CyclicBarrier} has to be instantiated outside this class, to perform an
 * {@code await().}F.E:
 * <pre>{@code
 *      CyclicBarrier barrier = new CyclicBarrier(2);
 *      ConnectionTestThread = new ConnectionTestThread(aPort, aHost, barrier);
 *      barrier.await();
 * }</pre>
 * This code will allow the process to stand by util connection with {@code aHost}
 * in port {@code aPort} is established and ready to use.
 * </p>
 */
public class ConnectionTestThread extends Thread {
    private final int port;
    private final String host;
    private final CyclicBarrier barrier;
    private boolean connectionAvailable;

    /**
     * Builds a ConnectionTestThread that would block the process it was instantiated in until
     * the connection with the given server is up.
     * @param port Server port
     * @param host Server host
     * @param barrier Previously instantiated {@link CyclicBarrier}
     */
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


    /**
     * Checks if the connection was successful or not
     * @return Returns true if there is no connection and false otherwise
     */
    public boolean isConnectionDown() {
        return !connectionAvailable;
    }
}
