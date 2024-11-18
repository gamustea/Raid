package raid.threads.localCommunication;

import raid.RS;
import raid.servers.*;
import raid.servers.files.strategies.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class LocalHearerThread extends Thread {
    private final int port;
    private final Strategy strategy;
    private Socket clientSocket;

    public LocalHearerThread(int port, Strategy strategy) {
        this.port = port;
        this.strategy = strategy;
    }

    @Override
    public void run() {
        Socket s = null;
        ServerSocket ss = null;
        try {
            ss = new ServerSocket(port);
            while (true) {
                try {
                    s = ss.accept();

                    ObjectInputStream ois = new ObjectInputStream(s.getInputStream());
                    ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());

                    int command = ois.readInt();
                    String message = null;
                    if (command == RS.SAVE_FILE) {
                        File file = (File) ois.readObject();
                        message = strategy.selfSaveFile(file);
                    }
                    if (command == RS.GET_FILE) {
                        String fileName = (String) ois.readObject();
                        message = strategy.selfGetFile(fileName);
                    }
                    if (command == RS.DELETE_FILE) {
                        String fileName = (String) ois.readObject();
                        message = strategy.selfDeleteFile(fileName);
                    }
                    oos.writeObject(message);
                    oos.flush();
                }
                catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                }
                finally {
                    Server.closeResource(s);
                }
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            Server.closeResource(ss);
        }
    }
}
