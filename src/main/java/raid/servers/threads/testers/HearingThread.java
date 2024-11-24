package raid.servers.threads.testers;

import static raid.misc.Util.*;

import java.io.IOException;
import java.net.ServerSocket;

public class HearingThread extends Thread{
    private final int port;

    public HearingThread(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        ServerSocket ss = null;

        try {
            ss = new ServerSocket(port);
            while (isAlive()) {
                try {
                    ss.accept();
                }
                catch (IOException e) {
                    System.out.println(e.getMessage());
                }
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
