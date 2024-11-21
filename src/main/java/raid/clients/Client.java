package raid.clients;

import raid.RS;
import raid.clients.threads.RetrieverThread;
import raid.servers.CentralServer;
import raid.servers.Server;
import raid.servers.files.strategies.Strategy;
import returning.Result;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;


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

            ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(s.getInputStream());

            Result<Integer, String> result = getCommand();

            int command = result.getResult1();
            String fileName = result.getResult2();

            // Mando al servidor la operación a realizar
            oos.writeInt(command);
            oos.flush();
            while (command != RS.CLOSE_CONNECTION) {

                // Gestión del resultado obtenido del comando
                String message = manageCommand(command, fileName, oos, ois);
                System.out.println(message);

                // Petición de siguiente comando
                result = getCommand();
                command = result.getResult1();
                fileName = result.getResult2();

                // Mandar el siguiente commando
                oos.writeInt(command);
                oos.flush();
            }
        }
        catch (IOException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
        finally {
            Server.closeResource(s);
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
                return new Result<>(RS.CLOSE_CONNECTION, null);
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
                        result = new Result<>(RS.GET_FILE, fileName);
                        commandNotValid = false;
                        break;
                    case "Save":
                    case "SAVE":
                    case "save":
                        if (file.exists()) {
                            result = new Result<>(RS.SAVE_FILE, fileName);
                            commandNotValid = false;
                        }
                        else {
                            System.out.println("File not exists");
                        }
                        break;
                    case "Delete":
                    case "DELETE":
                    case "delete":
                        result = new Result<>(RS.DELETE_FILE, fileName);
                        commandNotValid = false;
                        break;
                    default:
                        System.out.println("| NOT VALID COMMAND |");
                }
            }
        }

        return result;
    }

    private static String manageCommand(int command, String fileName, ObjectOutputStream oos, ObjectInputStream ois) throws IOException, ClassNotFoundException {
        String message = "";
        switch (command) {
            case RS.GET_FILE: {
                oos.writeObject(fileName);
                oos.flush();
                RetrieverThread retrieverThread1 = new RetrieverThread(60000, path);
                RetrieverThread retrieverThread2 = new RetrieverThread(60001, path);

                message = (String) ois.readObject();
                if (message.equals("| COMPLETED |")) {
                    while (retrieverThread1.getResult() != RS.NOT_READY &&
                            retrieverThread2.getResult() != RS.NOT_READY) {}

                    File file1 = retrieverThread1.getResultFile();
                    File file2 = retrieverThread2.getResultFile();

                    File finalFile = new File(path + "\\" + getCorrectFileName(file1.getName()));
                }
                else {
                    message = "| FAILURE |";
                }

                break;
            }
            case RS.DELETE_FILE: {
                oos.writeObject(fileName);
                oos.flush();
                message = (String) ois.readObject();
                break;
            }
            case RS.SAVE_FILE: {
                oos.writeObject(new File(fileName));
                oos.flush();
                message = (String) ois.readObject();
                break;
            }
        }

        return message;
    }

    public static void depositeContent(File file1, File file2) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file1));
        BufferedWriter bw = new BufferedWriter(new FileWriter(file2));

        String linea;
        while ((linea = br.readLine()) != null) {
            bw.write(linea);
        }
        bw.flush();
    }

    private static String getCorrectFileName(String fileName) {
        String extension = Strategy.getFileNameAndExtension(fileName).getResult2();
        String trueName = "";

        String[] parts1 = fileName.split("_1");
        String[] parts2 = fileName.split("_2");

        if (parts1.length == 2) {
            trueName = parts1[0] + extension;
        }
        else {
            trueName = parts2[0] + extension;
        }

        return trueName;
    }
}
