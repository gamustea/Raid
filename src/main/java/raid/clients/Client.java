package raid.clients;

import raid.misc.Util;
import raid.clients.threads.RetrieverThread;
import raid.servers.Server;
import raid.servers.files.Strategy;
import raid.misc.Result;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

import static java.lang.Thread.sleep;
import static raid.misc.Util.*;


public class Client {
    private final String host;
    private final int port;
    private static final String path = "C:\\Users\\gmiga\\Documents\\RaidTesting\\Client";

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void boot() {
        Socket s = null;

        try {
            System.out.println("Starting connection");
            s = new Socket(host, port);
            System.out.println("| Connection successful |");

            ObjectOutputStream clientOut = new ObjectOutputStream(s.getOutputStream());
            ObjectInputStream clientIn = new ObjectInputStream(s.getInputStream());

            Result<Integer, String> result = getCommand();

            int command = result.getResult1();
            String fileName = result.getResult2();

            // Mando al servidor la operación a realizar
            clientOut.writeInt(command);
            clientOut.flush();
            while (command != Util.CLOSE_CONNECTION) {

                // Gestión del resultado obtenido del comando
                int message = manageCommand(command, fileName, clientOut, clientIn);
                System.out.println(translateMessage(message));

                // Petición de siguiente comando
                result = getCommand();
                command = result.getResult1();
                fileName = result.getResult2();

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
                        if (file.exists()) {
                            result = new Result<>(Util.SAVE_FILE, fileName);
                            commandNotValid = false;
                        }
                        else {
                            System.out.println("File not exists");
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


    private static int manageCommand(int command, String fileName, ObjectOutputStream clientOut, ObjectInputStream clientIn) throws IOException, ClassNotFoundException, InterruptedException {
        int message = NOT_READY;

        switch (command) {
            case GET_FILE: {
                RetrieverThread retrieverThread1 = new RetrieverThread(Integer.parseInt(getProperty("CLIENT_HEAR_PORT1", Server.PORTS)), path);
                RetrieverThread retrieverThread2 = new RetrieverThread(Integer.parseInt(getProperty("CLIENT_HEAR_PORT2", Server.PORTS)), path);

                clientOut.writeObject(fileName);
                clientOut.flush();

                message = clientIn.readInt();
                if (message == FILE_RETRIEVED) {
                    retrieverThread1.start();
                    retrieverThread2.start();

                    boolean notReady1 = retrieverThread1.getResult() == NOT_READY;
                    boolean notReady2 = retrieverThread1.getResult() == NOT_READY;
                    int loopCount = 0;
                    while (notReady1 || notReady2) {
                        notReady1 = retrieverThread1.getResult() == NOT_READY;
                        notReady2 = retrieverThread1.getResult() == NOT_READY;
                        if (loopCount == 0) {
                            System.out.println("| THREADS NOT READY |");
                        }
                        loopCount++;
                    }
                    System.out.println("| THREADS NOT READY |");

                    File file1 = retrieverThread1.getResultFile();
                    File file2 = retrieverThread2.getResultFile();

                    File finalFile = new File(path + "\\" + getCorrectFileName(file1.getName()));

                    mergeFiles(file1, file2, finalFile);
                    file1.delete();
                    file2.delete();
                }
                else {
                    retrieverThread1.closeResources();
                    retrieverThread2.closeResources();
                }

                break;
            }
            case DELETE_FILE: {
                clientOut.writeObject(fileName);
                clientOut.flush();
                message = clientIn.readInt();
                break;
            }
            case SAVE_FILE: {
                clientOut.writeObject(fileName);
                clientOut.flush();

                byte[] buffer = new byte[1024];
                int bytesRead;
                FileInputStream fileReader = new FileInputStream(fileName);
                while ((bytesRead = fileReader.read(buffer)) != -1) {
                    clientOut.write(buffer, 0, bytesRead);
                    clientOut.flush();
                }

                closeResource(fileReader);
                message = clientIn.readInt();
                break;
            }
        }

        return message;
    }


    private static String getCorrectFileName(String fileName) {
        String extension = Strategy.getFileNameAndExtension(fileName).getResult2();
        String trueName;

        String[] parts1 = fileName.split("_1");
        String[] parts2 = fileName.split("_2");

        if (parts1.length == 2) {
            trueName = parts1[0] + "." + extension;
        }
        else {
            trueName = parts2[0] + "." + extension;
        }

        return trueName;
    }


    private static void mergeFiles(File file1, File file2, File destination) throws IOException {
        try (BufferedOutputStream fileWriter = new BufferedOutputStream(new FileOutputStream(destination))) {
            byte[] buffer = new byte[1024];
            int bytesRead;

            // Copiar contenido del primer archivo
            try (BufferedInputStream fileReader = new BufferedInputStream(new FileInputStream(file1))) {
                while ((bytesRead = fileReader.read(buffer)) != -1) {
                    fileWriter.write(buffer, 0, bytesRead);
                }
            }

            // Copiar contenido del segundo archivo
            try (BufferedInputStream fileReader = new BufferedInputStream(new FileInputStream(file2))) {
                while ((bytesRead = fileReader.read(buffer)) != -1) {
                    fileWriter.write(buffer, 0, bytesRead);
                }
            }
        }
    }
}
