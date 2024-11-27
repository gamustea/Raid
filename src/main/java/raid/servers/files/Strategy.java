package raid.servers.files;

import raid.misc.Util;
import raid.servers.Server;
import raid.servers.WestServer;
import raid.servers.threads.testers.ConnectionTestThread;
import raid.misc.Result;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;

import static raid.misc.Util.*;


public abstract class Strategy {
    protected ConnectionTestThread connectionTestLeft;
    protected ConnectionTestThread connectionTestRight;
    protected Path path;
    protected StrategyType strategyType;

    protected final static int WEST_LOCAL_CONNECTION_PORT = Integer.parseInt(
            getProperty("WEST_LOCAL_CONNECTION_PORT", Server.PORTS));
    protected final static int CENTRAL_LOCAL_CONNECTION_PORT = Integer.parseInt(
            getProperty("CENTRAL_LOCAL_CONNECTION_PORT", Server.PORTS));
    protected final static int EAST_LOCAL_CONNECTION_PORT = Integer.parseInt(
            getProperty("EAST_LOCAL_CONNECTION_PORT", Server.PORTS));

    public abstract int saveFile(File file);
    public abstract int deleteFile(String file);
    public abstract int getFile(String file, String clientHost);

    protected Strategy(String pathName, StrategyType strategyType) {
        path = Path.of(pathName);
        checkPathExistence(path);
        selectLocalConnections(strategyType);
    }


    /**
     * Method that allows a {@link Server} instance to save a given {@link File}
     * in its file storaging path.
     * @param file {@link File} to store
     * @return "SAVED" if the file was successfully stored
     */
    public int selfSaveFile(File file)  {
        File storedFile = new File(path + "\\" + file.getName());
        byte[] buffer = new byte[MAX_BUFFER];
        int bytesRead;

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;

        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            bos = new BufferedOutputStream(new FileOutputStream(storedFile));

            while ((bytesRead = bis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            bos.flush();
        }
        catch(IOException e) {
            System.out.println(e.getMessage());
        }
        finally {
            Util.closeResource(bis);
            Util.closeResource(bos);
        }

        return Util.FILE_STORED;
    }


    /**
     * Method that allows a {@link Server} instance to remove a given {@link File}
     * from its file storaging path.
     * @param fileName Name of the file to delete
     * @return "DELETED" if the file was successfully stored
     */
    public int selfDeleteFile(String fileName) {
        String realFileName = path + "\\" + fileName;
        File fileToDelete = new File(realFileName);

        if (fileToDelete.exists()) {
            if (fileToDelete.delete()) {
                return Util.FILE_DELETED;
            }
            return Util.CRITICAL_ERROR;
        }

        return Util.FILE_NOT_FOUND;
    }


    public int selfGetFile(String fileName, String clientHost, int port) {
        Socket socket = null;
        int message = NOT_READY;
        ObjectOutputStream strategyOut = null;

        try {
            boolean notConnected = true;
            while (notConnected) {
                try {
                    socket = new Socket(clientHost, port);
                    notConnected = false;
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }

            strategyOut = new ObjectOutputStream(socket.getOutputStream());

            File fileToRetrieve = new File(path + "\\" + fileName);
            long fileLength = fileToRetrieve.length();

            strategyOut.writeObject(fileToRetrieve.getName());
            strategyOut.flush();
            strategyOut.writeLong(fileLength);
            strategyOut.flush();

            byte[] buffer = new byte[MAX_BUFFER];
            int bytesRead;
            FileInputStream fileReader = new FileInputStream(fileToRetrieve);
            while ((bytesRead = fileReader.read(buffer, 0, MAX_BUFFER)) != -1) {
                strategyOut.write(buffer, 0, bytesRead);
            }
            strategyOut.flush();

            closeResource(fileReader);
            message = FILE_RETRIEVED;
        }
        catch (IOException e) {
            message = CRITICAL_ERROR;
        } finally {
            closeResource(socket);
            closeResource(strategyOut);
        }

        return message;
    }


    // ====================== AUXILIARY METHODS =======================


    /**
     * Method that sleeps until the rest of the servers are up. A
     * message would be sent to the current {@link Server} if they
     * are not up.
     */
    protected void waitForConnections() {
        int count = 0;
        while (!connectionTestLeft.isConnectionAvailable() || !connectionTestRight.isConnectionAvailable()) {
            if (count == 0) {
                System.out.println("PERIPHERAL SERVERS ARE NOT UP");
                count++;
            }
        }
    }


    /**
     * Starts {@link ConnectionTestThread} instances of this specific {@link Server}
     * instance.
     */
    protected void bootConnections() {
        if (!connectionTestLeft.isAlive()) {
            connectionTestLeft.start();
        }
        if (!connectionTestRight.isAlive()) {
            connectionTestRight.start();
        }
    }


    private void selectLocalConnections(StrategyType strategyType) {
        this.strategyType = strategyType;

        switch (strategyType) {
            case Central: {
                this.connectionTestLeft = new ConnectionTestThread(Server.WEST_TEST_PORT, Server.WEST_HOST);
                this.connectionTestRight = new ConnectionTestThread(Server.EAST_TEST_PORT, Server.EAST_HOST);
                break;
            }
            case East: {
                this.connectionTestLeft = new ConnectionTestThread(Server.WEST_TEST_PORT, Server.WEST_HOST);
                this.connectionTestRight = new ConnectionTestThread(WestServer.CENTRAL_TEST_PORT, Server.CENTRAL_HOST);
            }
            case West: {
                this.connectionTestLeft = new ConnectionTestThread(WestServer.CENTRAL_TEST_PORT, Server.CENTRAL_HOST);
                this.connectionTestRight = new ConnectionTestThread(Server.EAST_TEST_PORT, Server.EAST_HOST);
                break;
            }
        }
    }
}
