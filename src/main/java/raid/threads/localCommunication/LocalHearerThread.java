package raid.threads.localCommunication;

import raid.misc.Util;
import raid.servers.Server;
import raid.servers.files.ProcessingStrategy;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import static raid.misc.Util.closeResource;


/**
 * This {@link Thread} instances are meant to run alongside the {@link Server} that
 * instantiated its purpose is to hear for request from other servers, so that whenever
 * a different instance of a server wants to retrieve an operation to this instance,
 * there is always a Thread hearing. In that way, another server sends a request to perform
 * and all the resources required to do so.
 */
public class LocalHearerThread extends Thread {
    private final int port;
    private final ProcessingStrategy strategy;


    /**
     * Builds a hearing thread in the given port and following a certain strategy,
     * paired with the Server that instantiated it.
     * @param port Port in which the thread will hear.
     * @param strategy {@link ProcessingStrategy} of the server.
     */
    public LocalHearerThread(int port, ProcessingStrategy strategy) {
        this.port = port;
        this.strategy = strategy;
    }


    @Override
    public void run() {
        Socket s = null;
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(port);
            while (this.isAlive()) {
                try {
                    s = ss.accept();

                    ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
                    ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());

                    int command = ois.readInt();
                    int message = Util.NOT_READY;

                    switch (command) {
                        case Util.SAVE_FILE: {
                            File file = (File) ois.readObject();
                            message = strategy.selfSaveFile(file);
                            break;
                        }
                        case Util.GET_FILE: {
                            String fileName = (String) ois.readObject();
                            String clientHost = (String) ois.readObject();
                            int clientPort = ois.readInt();
                            message = strategy.selfGetFile(fileName, clientHost, clientPort);
                            break;
                        }
                        case Util.DELETE_FILE: {
                            String fileName = (String) ois.readObject();
                            message = strategy.selfDeleteFile(fileName);
                            break;
                        }
                    }

                    oos.writeInt(message);
                    oos.flush();
                }
                catch (ClassNotFoundException | IOException e) {
                    System.out.println(e.getMessage());
                }
                finally {
                    closeResource(s);
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            closeResource(ss);
        }
    }
}
