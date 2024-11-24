package raid.clients.threads;

import static raid.misc.Util.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RetrieverThread extends Thread {
    private ServerSocket listenerSocket;
    private final String path;
    private int result = NOT_READY;
    private File resultFile = null;

    public RetrieverThread(int port, String path) throws IOException {
        if (this.listenerSocket == null) {
            listenerSocket = new ServerSocket(port);
        }
        this.path = path;
        checkPathExistence(Paths.get(path));
    }

    @Override
    public void run() {
        ObjectInputStream ois = null;
        FileOutputStream fileWriter = null;
        Socket socket = null;

        try {
            // Recibe la petición de uno de los servidores
            socket = listenerSocket.accept();
            ois = new ObjectInputStream(socket.getInputStream());

            // Obtiene el fichero del servidor y lo trata
            File fileToStore = (File) ois.readObject();
            File storedFile = new File(path + "\\" + fileToStore.getName());

            long fileSize = ois.readLong();
            if (fileSize != -1) {
                if (!storedFile.exists()) {
                    Files.createFile(storedFile.toPath());
                }

                fileWriter = new FileOutputStream(storedFile);
                byte[] buffer = new byte[(int) fileSize];

                int bytesRead = ois.read(buffer, 0, (int) fileSize);
                fileWriter.write(buffer);
                fileWriter.flush();
            }

            resultFile = storedFile;
            result = FILE_STORED;
        }
        catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        finally {
            closeResource(listenerSocket);
            closeResource(socket);
            closeResource(ois);
            closeResource(fileWriter);
        }
    }

    public int getResult() {
        return result;
    }

    public File getResultFile() {
        return resultFile;
    }

    public void closeResources() {
        closeResource(listenerSocket);
        listenerSocket = null;
        this.interrupt();
    }
}
