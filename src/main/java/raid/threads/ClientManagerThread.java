package raid.threads;

import raid.misc.Result;
import raid.misc.Util;
import raid.servers.Server;
import raid.servers.files.ProcessingStrategy;

import static raid.misc.Util.*;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * {@link Thread} with the purpose of managing user requests, such as deleting,
 * storing or retrieving files. It works
 * by getting an already built {@link Socket} —directly communicated with
 * the respective user— and an {@link ProcessingStrategy}, associated with the server
 * hosting the thread.
 */
public class ClientManagerThread extends Thread {
    private final Socket clientSocket;
    private final ProcessingStrategy strategy;
    private final String clientHost;

    /**
     * Builds a specialized {@link Thread} instance, by allowing the communication
     * with a specific client. It stores an {@link ProcessingStrategy} depending on the server
     * hosting the thread.
     * @param socket {@link Socket} of the client
     * @param strategy {@link ProcessingStrategy} to follow
     */
    public ClientManagerThread(Socket socket, ProcessingStrategy strategy) {
        this.clientSocket = socket;
        this.strategy = strategy;
        this.clientHost = socket.getInetAddress().getCanonicalHostName();
    }


    /**
     * <p>Executes de {@code ClientManagerThread} {@code run()} method, so that it begins
     * the communication between the client and the {@link Server} hosting the thread
     * and connection. By starting this method, the server would ask for an instruction
     * -as a {@link String}- to perform, which can be one of the respective
     * (capital letters are allowed):</p>
     * <ul>
     *     <li>{@code Save <FILE_PATH>}: server will perform its strategy for storing the
     *     given {@link File} instance</li>
     *     <li>{@code Delete <FILE_NAME>}: server will delete a file withe the given name</li>
     *     <li>{@code Get <FILE_NAME>}: retrieves a File with the given name</li>
     *     <li>{@code Close}: Closes connection between Server and User</li>
     * </ul>
     */
    @Override
    public void start() {
        super.start();
    }


    @Override
    public void run() {
        try {
            String temporalPath = SERVER_FILE_PATH + "\\Auxiliar";

            // Abre streams para comunicarse con el cliente, usando su socket
            ObjectOutputStream serverOut = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream serverIn = new ObjectInputStream(clientSocket.getInputStream());

            // Recibe el comando, y si es "CLOSE", directamente lo descarta
            int command = serverIn.readInt();
            while (command != Util.CLOSE_CONNECTION) {
                String fileNameReceived = (String) serverIn.readObject();

                // Filtra por el tipo de comando recibido y ejecuta el método correspondiente
                int message = NOT_READY;
                switch(command) {
                    case GET_FILE: {
                        // Le devuelve al cliente el resultado de la operación de obtención
                        message = strategy.getFile(fileNameReceived, clientHost);
                        break;
                    }
                    case SAVE_FILE: {
                        checkPathExistence(Path.of(temporalPath));
                        File file = new File(temporalPath + "\\" + fileNameReceived);
                        if (!file.exists()) {
                            Files.createFile(file.toPath());
                        }

                        // Escribir en un archivo local lo que venga del cliente
                        FileOutputStream fileWriter = new FileOutputStream(file);
                        long pendantReading = serverIn.readLong();
                        byte[] buffer = new byte[MAX_BUFFER];

                        while (pendantReading != 0) {
                            int bytesRead = serverIn.read(buffer, 0, Math.min(MAX_BUFFER, (int) pendantReading));
                            fileWriter.write(buffer, 0, bytesRead);
                            pendantReading -= bytesRead;
                        }
                        fileWriter.flush();

                        closeResource(fileWriter);

                        // Devuelve al cliente el resultado de realizar la operación
                        message = strategy.saveFile(file);
                        break;
                    }
                    case DELETE_FILE: {
                        message = strategy.deleteFile(fileNameReceived);
                        break;
                    }
                    case STAND_BY: {
                        message = STANDING_BY;
                        break;
                    }
                    case LIST_FILES: {
                        Result<Integer, List<String>> result = strategy.listFiles();
                        message = result.result1();
                        serverOut.writeObject(result.result2());
                    }
                }

                // Mandar el resultado de la operación al cliente
                serverOut.writeInt(message);
                serverOut.flush();
                deleteDirectory(new File(temporalPath));
                command = serverIn.readInt();
            }
        }
        catch (ClassNotFoundException | IOException e) {
            System.out.println(e.getMessage());
        }
        finally {
            System.out.println("| Client closed |");
            closeResource(clientSocket);
        }
    }
}
