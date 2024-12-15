package raid.clients;

import raid.misc.Util;
import raid.threads.RetrieverThread;
import raid.servers.Server;
import raid.misc.Result;
import raid.threads.testers.HearingThread;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

import static raid.misc.Util.*;


/**
 * Instance of an Object that allows a user to connect to any
 * instance of {@link Server}, no matter its type, and
 * managing the files that the server stores.
 */
public class Client {
    private final String host;
    private final int port;
    private ObjectOutputStream clientOut;
    private ObjectInputStream clientIn;
    private static final String path = CLIENT_FILE_PATH;

    private boolean noBoot = false;


    /**
     * Builds a client with the given parameters
     * @param host {@link Server} host
     * @param port {@link Server} port
     */
    public Client(String host, int port) {
        if (!(new File(CLIENT_FILE_PATH).exists())) {
            System.out.println("THIS CLIENT WON'T BOOT - CHECK PATH EXISTENCE AND BUILD A NEW CLIENT OR EXECUTE AGAIN");
            noBoot = true;
            this.host = null;
            this.port = 0;
        } else {
            this.host = host;
            this.port = port;
            checkPathExistence(Path.of(path));
        }
    }


    /**
     * Starts up a console menu for the user. They are given a prompt
     * to introduce commands and a manual to read.
     */
    public void boot() {
        if (noBoot) {
            return;
        }

        Socket s = null;

        try {
            System.out.println("Starting connection");
            s = new Socket(host, port);
            System.out.println("\n|========| USER INTERFACE (Connection successful) |========|");
            System.out.println("        Welcome to this storaging Raid System.\n        You are currently connected to a Server instance");
            System.out.println("        For any advice in the use of this\n        software, type \"MAN\" in the prompt below; ");
            System.out.println("        otherwise, you are ready to begin.\n");

            clientOut = new ObjectOutputStream(s.getOutputStream());
            clientIn = new ObjectInputStream(s.getInputStream());

            HearingThread hearingThread = new HearingThread(Integer.parseInt(getProperty("CLIENT_TEST_PORT", PORTS)));
            hearingThread.start();

            Result<Integer, String> result = getCommand();

            int command = result.result1();
            String fileName = result.result2();

            // Mando al servidor la operación a realizar
            clientOut.writeInt(command);
            clientOut.flush();
            while (command != Util.CLOSE_CONNECTION) {

                // Gestión del resultado obtenido del comando
                int message = manageCommand(command, fileName);
                System.out.println(translateMessage(message));

                // Petición de siguiente comando
                result = getCommand();
                command = result.result1();
                fileName = result.result2();

                // Mandar el siguiente commando
                clientOut.writeInt(command);
                clientOut.flush();
            }
        }
        catch (IOException | ClassNotFoundException | InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            closeResource(s);
        }
    }


    /**
     * Gets the code for a command based on users keyboard input.
     * Checks whether the typed command is correct or not.
     * @return {@link Result} that stores the commando code from
     * {@link Util} and the File name (if needed)
     */
    private static Result<Integer, String> getCommand() {
        Result<Integer, String> result = null;
        Scanner scanner = new Scanner(System.in);
        boolean commandNotValid = true;

        while (commandNotValid) {
            System.out.print("--> ");
            String command = scanner.nextLine();

            // Si el comando es Close, sale directamente
            if (command.equalsIgnoreCase("close")) {
                return new Result<>(Util.CLOSE_CONNECTION, null);
            }

            if (command.equalsIgnoreCase("man")) {
                return new Result<>(STAND_BY, "TEXTO_DE_EJEMPLO");
            }

            if (command.equalsIgnoreCase("list")) {
                return new Result<>(LIST_FILES, "TEXTO_DE_EJEMPLO");
            }

            // Divide el contenido de la String
            String[] parts = command.split(" ");

            // Si no ha escrito justamente dos palabras,
            // el comando no es válido
            if (parts.length != 2) {
                System.out.println("| COMMAND NOT VALID |");
            }
            else {
                String order = parts[0];
                String fileName = parts[1];

                File file = new File(fileName);

                switch (order) {
                    case "GET":
                    case "Get":
                    case "get":
                        result = new Result<>(Util.GET_FILE, fileName);
                        commandNotValid = false;
                        break;
                    case "Save":
                    case "SAVE":
                    case "save":
                        if (!file.exists() || file.isDirectory()) {
                            System.out.println(translateMessage(FILE_NOT_FOUND));
                        }
                        else {
                            result = new Result<>(Util.SAVE_FILE, fileName);
                            commandNotValid = false;
                        }
                        break;
                    case "Delete":
                    case "DELETE":
                    case "delete":
                        result = new Result<>(Util.DELETE_FILE, fileName);
                        commandNotValid = false;
                        break;
                    default:
                        System.out.println("| NOT VALID COMMAND |");
                }
            }
        }

        return result;
    }


    /**
     * Based on the given command, manages it. It can store, delete, list or
     * get files, by communicating with the external host.
     * @param command {@link Util} command code, representing the instruction to perform
     * @param fileName Name of the file to manage
     * @return A {@link Util} code based on the successfulness of the operation
     * @throws IOException If an I/O Exception occurred
     * @throws ClassNotFoundException If a retrieved object has an unknown class
     * @throws InterruptedException If the thread was interrputed
     */
    private int manageCommand(int command, String fileName) throws IOException, ClassNotFoundException, InterruptedException {
        int message = NOT_READY;

        // Manda al servidor el nombre del archivo con el que operar,
        // instrucción común a todos los tipos de operaciones
        clientOut.writeObject(new File(fileName).getName());
        clientOut.flush();

        switch (command) {
            case GET_FILE: {
                RetrieverThread retrieverThread1 = new RetrieverThread(Integer.parseInt(getProperty("CLIENT_HEAR_PORT1", PORTS)), path);
                RetrieverThread retrieverThread2 = new RetrieverThread(Integer.parseInt(getProperty("CLIENT_HEAR_PORT2", PORTS)), path);

                // Recibe el mensaje de operación, y si resulta que el archivo no existe,
                // no realiza el resto de operaciones
                message = clientIn.readInt();
                if (message == SUCCESS) {
                    retrieverThread1.start();
                    retrieverThread2.start();

                    retrieverThread1.join();
                    retrieverThread2.join();

                    Result<String, String> result = getFileNameAndExtension(fileName);

                    File file1 = new File(path + "\\" + result.result1() + "_1." + result.result2());
                    File file2 = new File(path + "\\" + result.result1() + "_2." + result.result2());

                    File finalFile = new File(path + "\\" + getCorrectFileName(file1.getName()));
                    mergeFiles(file1, file2, finalFile);

                    file1.delete();
                    file2.delete();
                }

                // Cierra los recursos para volver a usarlos cuando toque
                retrieverThread1.closeResources();
                retrieverThread2.closeResources();
                break;
            }
            case DELETE_FILE: {
                message = clientIn.readInt();
                break;
            }
            case SAVE_FILE: {
                File fileToSend = new File(fileName);
                clientOut.writeLong(fileToSend.length());
                clientOut.flush();

                byte[] buffer = new byte[MAX_BUFFER];
                int bytesRead;
                FileInputStream fileReader = new FileInputStream(fileName);
                while ((bytesRead = fileReader.read(buffer)) != -1) {
                    clientOut.write(buffer, 0, bytesRead);
                }
                clientOut.flush();

                closeResource(fileReader);
                message = clientIn.readInt();
                break;
            }
            case LIST_FILES: {
                System.out.println("Listing files");
                showFiles((List<String>) clientIn.readObject());
                message = clientIn.readInt();
                break;
            }
            case STAND_BY: {
                message = clientIn.readInt();
                printManual();
                break;
            }
        }

        return message;
    }


    /**
     * Given a list of file names, prints on the screen all the names
     * stored.
     * @param fileNames {@link List} of {@link String} objects representing
     * existing file names
     */
    private static void showFiles(List<String> fileNames) {
        int counter = 1;
        System.out.println("\n|========== FILES AVAILABLE ========|\n");
        for (String fileName : fileNames) {
            System.out.println("  " +counter + ".- " + fileName);
            counter++;
        }
        System.out.println("\n|===================================|\n");
    }


    /**
     * Prints on the screen the user manual
     */
    private static void printManual() {
        System.out.println("""
                |======================================== USER MANUAL ========================================|
                |                                                                                             |
                |   This software supports multiple instructions in order to manipulate your files, and they  |
                | will be explained in the following lines:                                                   |
                |                                                                                             |
                |       · GET [FILE_NAME]: retrieves to the user the specified file with FILE_NAME name. Su-  |
                |         ch file would be deposited in the specified properties file (most accurately, in    |
                |         the path held by the variable "CLIENT_PATH") "resources/absoluteRoutes.propert-     |
                |         ies".                                                                               |
                |       · DELETE [FILE_NAME]: deletes an specified file from all servers named ass FILE_NAME  |
                |       · SAVE [FILE_NAME]: saves in all servers the specified file (WARNING: it ought to be  |
                |         an absolute route - otherwise, only files in the current project would be           |
                |         recognized)                                                                         |
                |       · LIST: prints all the currently available files stored in servers.                   |
                |                                                                                             |
                |   Errors might happen if the "SERVER_PATH" in "resources/absoluteRoutes.properties" is not  |
                | well written by the user. It is recommended to change it before the beginning of any test.  |
                | WARNING: It's not recommended to change server ports in "resources/ports.properties", as it |
                | may occasional trouble.                                                                     |
                |                                                                                             |
                |=============================================================================================|
           
                """);
    }
}
