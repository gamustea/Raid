package raid.threads.testers;

import raid.servers.Server;

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
                    e.printStackTrace();
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            Server.closeResource(ss);
        }
    }
}
