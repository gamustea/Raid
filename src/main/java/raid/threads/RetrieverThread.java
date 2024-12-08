package raid.threads;

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

    public RetrieverThread(int port, String path) throws IOException {
        listenerSocket = new ServerSocket(port);
        this.path = path;
        checkPathExistence(Paths.get(path));
    }

    @Override
    public void run() {
        ObjectInputStream threadIn = null;
        FileOutputStream fileWriter = null;
        Socket socket = null;

        try {
            // Recibe la petición de uno de los servidores
            socket = listenerSocket.accept();

            threadIn = new ObjectInputStream(socket.getInputStream());

            // Obtiene el fichero del servidor y lo trata
            String fileName = (String) threadIn.readObject();
            File storedFile = new File(path + "\\" + fileName);

            long pendantReading = threadIn.readLong();
            byte[] buffer = new byte[MAX_BUFFER];
            checkPathExistence(Paths.get(path));

            fileWriter = new FileOutputStream(storedFile);

            while (pendantReading != 0) {
                int bytesRead = threadIn.read(buffer, 0, Math.min(MAX_BUFFER, (int) pendantReading));
                fileWriter.write(buffer, 0, bytesRead);
                pendantReading -= bytesRead;
            }
            fileWriter.flush();

            result = FILE_STORED;
        }
        catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        finally {
            closeResource(socket);
            closeResource(fileWriter);
        }
    }

    public int getResult() {
        return result;
    }

    public void closeResources() {
        closeResource(listenerSocket);
        listenerSocket = null;
    }
}
