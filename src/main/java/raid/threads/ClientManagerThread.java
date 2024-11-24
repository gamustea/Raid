package raid.threads;

import raid.Util;
import raid.servers.Server;
import raid.servers.files.strategies.Strategy;

import static raid.Util.*;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;

/**
 * {@link Thread} with the purpose of managing user requests, such as deleting,
 * storing or retrieving files. It works
 * by getting an already built {@link Socket} —directly communicated with
 * the respective user— and an {@link Strategy}, associated with the server
 * hosting the thread.
 */
public class ClientManagerThread extends Thread {
    private final Socket clientSocket;
    private final Strategy strategy;
    private final String clientHost;
    private final String temporalPath = "C:\\Users\\gmiga\\Documents\\RaidTesting\\Auxiliar";

    /**
     * Builds a specialized {@link Thread} instance, by allowing the communication
     * with a specific client. It stores an {@link Strategy} depending on the server
     * hosting the thread.
     * @param socket {@link Socket} of the client
     * @param strategy {@link Strategy} to follow
     */
    public ClientManagerThread(Socket socket, Strategy strategy) {
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
            System.out.println(clientSocket.getInetAddress() + " has connected");

            // Abre streams para comunicarse con el cliente, usando su socket
            ObjectOutputStream serverOut = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream serverIn = new ObjectInputStream(new BufferedInputStream(clientSocket.getInputStream()));

            // Recibe el comando, y si es "CLOSE", directamente lo descarta
            int command = serverIn.readInt();
            while (command != Util.CLOSE_CONNECTION) {
                String fileNameReceived = (String) serverIn.readObject();

                // Filtra por el tipo de comando recibido y ejecuta el método correspondiente
                // según el Strategy insertado en el FileManager durante la construcción
                int message = Util.NOT_READY;
                switch(command) {
                    case GET_FILE: {
                        // Le devuelve al cliente el resultado de la operación de obtención
                        message = strategy.getFile(fileNameReceived, clientHost);
                        break;
                    }
                    case SAVE_FILE: {
                        long fileSize = serverIn.readLong();
                        File file = new File(temporalPath + "\\" + fileNameReceived);
                        if (!file.exists()) {
                            Files.createFile(file.toPath());
                        }

                        // Escribir en un archivo local lo que venga del cliente
                        DataOutputStream fileWriter = new DataOutputStream(new FileOutputStream(file));

                        byte[] buffer = new byte[(int) fileSize];
                        int bytesRead= serverIn.read(buffer, 0, (int) fileSize);

                        fileWriter.write(buffer, 0, bytesRead);
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
                }

                // Mandar el resultado de la operación al cliente
                serverOut.writeInt(message);
                serverOut.flush();
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
