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
            e.printStackTrace();
            return "| ERROR WHILE STORAGING |";
        }
        finally {
            connectionTestLeft.interrupt();
            connectionTestRight.interrupt();

            if (westServerSocket != null) {
                try {
                    westServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (eastServerSocket != null) {
                try {
                    eastServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        selfSaveFile(file);

        return "| FILE SAVED |";
    }

    @Override
    public String deleteFile(String fileName) {

        waitForConnection();



        selfDeleteFile(fileName);
        return "| FILE COMPLETELY DELETED |";
    }

    @Override
    public String getFile(String file, Socket clientSocket) {
        return null;
    }
}
