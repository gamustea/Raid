package raid.servers.threads;

import raid.servers.*;
import raid.servers.files.strategies.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class LocalCommunication extends Thread {
    private final int port;
    private final Strategy strategy;
    private Socket clientSocket;

    public LocalCommunication(int port, Strategy strategy) {
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
                    if (command == Server.SAVE_FILE) {
                        File file = (File) ois.readObject();
                        strategy.selfSaveFile(file);
                        oos.writeBytes("SAVE COMPLETED");
                    }
                    if (command == Server.GET_FILE) {
                        String fileName = (String) ois.readObject();
                        oos.writeObject(strategy.selfGetFile(fileName, clientSocket));
                    }
                    if (command == Server.DELETE_FILE) {
                        String fileName = (String) ois.readObject();
                        strategy.selfDeleteFile(fileName);
                        oos.writeObject("DELETED COMPLETED");
                    }
                    oos.flush();
                }
                catch (ClassNotFoundException | IOException e) {
                    e.printStackTrace();
                }
                finally {
                    if (s != null) {
                        try {
                            s.close();
                        }
                        catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            if (ss != null) {
                try {
                    ss.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
