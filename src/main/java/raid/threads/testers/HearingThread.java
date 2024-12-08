package raid.threads.testers;

import static raid.misc.Util.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Paired with the {@link ConnectionTestThread}, allows that thread
 * to determine whether a connection is able to establish or not. This
 * thread, will run and wait for a {@link Socket} until it is interrupted.
 */
public class HearingThread extends Thread{
    private final int port;

    /**
     * Builds a hearing thread in the given port.
     * @param port Port to hear.
     */
    public HearingThread(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        ServerSocket ss = null;

        try {
            ss = new ServerSocket(port);
            while (!Thread.interrupted()) {
                Socket socket = null;
                try {
                    socket = ss.accept();
                }
                catch (IOException e) {
                    System.out.println(e.getMessage());
                }
                closeResource(socket);
            }
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
        finally {
            closeResource(ss);
        }
    }
}
