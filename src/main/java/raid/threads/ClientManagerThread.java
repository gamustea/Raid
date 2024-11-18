package raid.threads;

import raid.RS;
import raid.servers.files.FileManager;
import raid.servers.Server;
import raid.servers.files.strategies.Strategy;

import java.io.*;
import java.net.Socket;

/**
 * {@link Thread} with the purpose of managing user requests, such as deleting,
 * storing or retrieving files. It works
 * by getting an already built {@link Socket} —directly communicated with
 * the respective user— and an {@link Strategy}, associated with the server
 * hosting the thread.
 */
public class ClientManagerThread extends Thread {
    private final Socket clientSocket;
    private final FileManager fileManager;

    /**
     * Builds a specialized {@link Thread} instance, by allowing the communication
     * with a specific client. It stores an {@link Strategy} depending on the server
     * hosting the thread.
     * @param socket {@link Socket} of the client
     * @param strategy {@link Strategy} to follow
     */
    public ClientManagerThread(Socket socket, Strategy strategy) {
        this.clientSocket = socket;
        this.fileManager = new FileManager(strategy);
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
            ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());

            // Recibe el comando, y si es "CLOSE", directamente lo descarta
            int command = ois.readInt();
            while (command != RS.CLOSE_CONNECTION) {
                File file;
                String fileName;

                // Filtra por el tipo de comando recibido y ejecuta el método correspondiente
                // según el Strategy insertado en el FileManager durante la construcción
                String message = "";
                switch(command) {
                    case RS.GET_FILE: {

                        // Recibe del cliente el nombre del archivo que quiere recibir
                        fileName = (String) ois.readObject();

                        // Le devuelve al cliente el resultado de la operación de obtención
                        message = fileManager.getFile(fileName, clientSocket);
                        break;
                    }
                    case RS.SAVE_FILE: {

                        // Recibe del cliente el objeto File que quiere guardar
                        file = (File) ois.readObject();

                        // Devuelve al cliente el resultado de realizar la operación
                        message = fileManager.saveFile(file);
                        break;
                    }
                    case RS.DELETE_FILE: {
                        fileName = (String) ois.readObject();
                        message = fileManager.deleteFile(fileName);
                        break;
                    }
                }

                oos.writeObject(message);
                oos.flush();
                command = ois.readInt();
            }
        }
        catch (ClassNotFoundException | IOException e) {
            System.out.println(e.getMessage());
        }
        finally {
            System.out.println("| Client closed |");
            Server.closeResource(clientSocket);
        }
    }
}
