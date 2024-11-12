package raid.servers.files.strategies;

import raid.servers.EastServer;
import raid.servers.Server;
import raid.servers.WestServer;
import returning.Result;

import java.io.*;
import java.net.Socket;


import static java.lang.Thread.sleep;

public class FullSavingStrategy extends Strategy {
    public FullSavingStrategy(String path) {
        super(path, StrategyType.Central);
    }

    @Override
    public String saveFile(File file) {
        System.out.println("| STARTING TO SAVE " + file.getName() + " |\n");

        Socket westServerSocket = null;
        Socket eastServerSocket = null;

        bootConnections();
        waitForConnection();

        try {
            westServerSocket = new Socket(Server.WEST_HOST, Strategy.WEST_LOCAL_CONNECTION_PORT);
            eastServerSocket = new Socket(Server.EAST_HOST, Strategy.EAST_LOCAL_CONNECTION_PORT);
            Result<File, File> result = splitFile(
                    file,
                    file.getName() + "_1",
                    file.getName() + "_2"
            );

            // Crear los streams para el primer servidor parcial
            ObjectOutputStream oos = new ObjectOutputStream(westServerSocket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(westServerSocket. getInputStream());

            // Pedirle al primer servidor que guarde la primera mitad
            // del fichero y recibir un mensaje
            oos.writeInt(WestServer.SAVE_FILE);
            oos.flush();
            oos.writeObject(result.getResult1());
            oos.flush();
            System.out.println(ois.readLine());

            // Crear los streams para el segundo servidor parcial
            oos = new ObjectOutputStream(eastServerSocket.getOutputStream());
            ois = new ObjectInputStream(eastServerSocket. getInputStream());

            // Pedirle al segundo servidor que guarde la segunda mitad
            // del fichero y recibir un mensaje
            oos.writeInt(EastServer.SAVE_FILE);
            oos.flush();
            oos.writeObject(result.getResult2());
            oos.flush();
            System.out.println(ois.readLine());
        }
        catch (IOException e) {
            return "| ERROR WHILE STORAGING |";
        }
        finally {
            Server.closeResource(connectionTestLeft);
            Server.closeResource(connectionTestRight);

            Server.closeResource(westServerSocket);
            Server.closeResource(eastServerSocket);
        }
        selfSaveFile(file);

        return "| FILE SAVED |";
    }

    @Override
    public String deleteFile(String fileName) {
        System.out.println("| STARTING TO DELETE " + fileName + " |\n");

        bootConnections();
        waitForConnection();

        String finalMessage = selfDeleteFile(fileName);

        Socket westServerSocket = null;
        Socket eastServerSocket = null;

        if (!finalMessage.equals("| ERROR FILE NOT FOUND |")) {
            try {
                westServerSocket = new Socket(Server.WEST_HOST, Strategy.WEST_LOCAL_CONNECTION_PORT);
                eastServerSocket = new Socket(Server.EAST_HOST, Strategy.EAST_LOCAL_CONNECTION_PORT);
                String fileName1 = fileName + "_1";
                String fileName2 = fileName + "_2";

                // Crear los streams para el primer servidor parcial
                ObjectOutputStream oos = new ObjectOutputStream(westServerSocket.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(westServerSocket. getInputStream());

                // Pedirle al primer servidor que borre la primera mitad
                // del fichero y recibir un mensaje
                oos.writeInt(WestServer.DELETE_FILE);
                oos.flush();
                oos.writeObject(fileName1);
                oos.flush();
                System.out.println((String) ois.readObject());

                // Crear los streams para el segundo servidor parcial
                oos = new ObjectOutputStream(eastServerSocket.getOutputStream());
                ois = new ObjectInputStream(eastServerSocket. getInputStream());

                // Pedirle al segundo servidor que borre la segunda mitad
                // del fichero y recibir un mensaje
                oos.writeInt(EastServer.DELETE_FILE);
                oos.flush();
                oos.writeObject(fileName2);
                oos.flush();
                System.out.println((String) ois.readObject());

                finalMessage = "| FILE COMPLETELY DELETED |";
            }
            catch (ClassNotFoundException | IOException e) {
                return "| ERROR WHILE STORAGING |";
            }
            finally {
                Server.closeResource(connectionTestLeft);
                Server.closeResource(connectionTestRight);

                Server.closeResource(westServerSocket);
                Server.closeResource(eastServerSocket);
            }
        }

        return finalMessage;
    }

    @Override
    public String getFile(String file) {
        return null;
    }
}
