package raid.clients.threads;

import raid.RS;
import raid.clients.Client;
import raid.servers.Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class RetrieverThread extends Thread {
    private final ServerSocket listenerSocket;
    private final String path;
    private int result = RS.NOT_READY;
    private File resultFile = null;

    public RetrieverThread(int port, String path) throws IOException {
        listenerSocket = new ServerSocket(port);
        this.path = path;
    }

    @Override
    public void run() {
        ObjectInputStream oos = null;

        try {
            Socket socket = listenerSocket.accept();
            oos = new ObjectInputStream(socket.getInputStream());

            File fileToStore = (File) oos.readObject();
            File storedFile = new File(path + "\\" + fileToStore.getName());

            Client.depositeContent(fileToStore, storedFile);


            result = RS.FILE_STORED;
        }
        catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        finally {
            Server.closeResource(oos);
        }
    }

    public int getResult() {
        return result;
    }

    public File getResultFile() {
        return resultFile;
    }
}
