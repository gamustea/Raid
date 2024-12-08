package raid.threads;

import raid.clients.Client;
import raid.misc.Util;

import static raid.misc.Util.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Paths;


/**
 * {@link Client} exclusive thread that allows a hearing strategy. This means
 * that other servers that want to simultaneously send files are able to do so.
 */
public class RetrieverThread extends Thread {
    private ServerSocket listenerSocket;
    private final String path;
    private int result = NOT_READY;


    /**
     * Builds a retriever thread in a client, that will hear for
     * operations from other servers.
     * @param port Port at which this client will be hearing
     * @param path File path
     * @throws IOException If an I/O exception happens
     */
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
            result = CRITICAL_ERROR;
            e.printStackTrace();
        }
        finally {
            closeResource(socket);
            closeResource(fileWriter);
        }
    }


    /**
     * Return the current result based on the status of the process.
     * @return {@link Util} for critical error or file stored.
     */
    public int getResult() {
        return result;
    }


    /**
     * Closes the {@link ServerSocket} in the thread, so that
     * it can be instantiated again when need and the port
     * remains unused.
     */
    public void closeResources() {
        closeResource(listenerSocket);
        listenerSocket = null;
    }
}
