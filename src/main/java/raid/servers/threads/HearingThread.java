package raid.servers.threads;

import raid.servers.Server;

import javax.imageio.IIOException;
import java.io.IOException;
import java.net.ServerSocket;

public class HearingThread extends Thread{
    private int port;

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
