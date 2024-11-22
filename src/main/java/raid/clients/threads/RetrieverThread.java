package raid.clients.threads;

import static raid.Util.*;
import raid.clients.Client;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RetrieverThread extends Thread {
    private final ServerSocket listenerSocket;
    private final String path;
    private int result = NOT_READY;
    private File resultFile = null;

    public RetrieverThread(int port, String path) throws IOException {
        listenerSocket = new ServerSocket(port);
        this.path = path;
        checkPathExistence(Paths.get(path));
    }

    @Override
    public void run() {
        ObjectInputStream oos = null;

        try {
            // Recibe la petición de uno de los servidores
            Socket socket = listenerSocket.accept();
            oos = new ObjectInputStream(socket.getInputStream());

            // Obtiene el fichero del servidor y lo trata
            File fileToStore = (File) oos.readObject();
            File storedFile = new File(path + "\\" + fileToStore.getName());
            if (!storedFile.exists()) {
                Files.createFile(storedFile.toPath());
            }

            depositContent(storedFile, fileToStore);
            resultFile = storedFile;
            result = FILE_STORED;
        }
        catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        finally {
            closeResource(oos);
        }
    }

    public int getResult() {
        return result;
    }

    public File getResultFile() {
        this.interrupt();
        return resultFile;
    }
}
