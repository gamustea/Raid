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
    public int selfSaveFile(File file) {
        File storedFile = new File(path + "\\" + file.getName());
        byte[] buffer = new byte[(int) (file.length() - 1)];

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;

        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            bos = new BufferedOutputStream(new FileOutputStream(storedFile));

            if (bis.read(buffer) != -1) {
                bos.write(buffer);
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
        ObjectOutputStream oos = null;

        try {
            boolean notConnected = true;
            while (notConnected) {
                try {
                    socket = new Socket("localhost", port);
                    notConnected = false;
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
            oos = new ObjectOutputStream(socket.getOutputStream());

            File fileToRetrieve = new File(path + "\\" + fileName);
            long fileLength = fileToRetrieve.length();

            oos.writeObject(fileToRetrieve);
            oos.flush();
            oos.writeLong(fileLength);
            oos.flush();

            byte[] buffer = new byte[(int) fileLength];
            FileInputStream fileReader = new FileInputStream(fileToRetrieve);
            if (fileReader.read(buffer, 0, (int) fileLength) != -1) {
                oos.write(buffer);
                oos.flush();
            }

            closeResource(fileReader);
            message = FILE_RETRIEVED;
        }
        catch (IOException e) {
            message = CRITICAL_ERROR;
        }
        finally {
            closeResource(socket);
            closeResource(oos);
        }

        return message;
    }


    // ====================== AUXILIARY METHODS =======================

    /**
     * Given a certain file, splits it in half and returns it as two new Files,
     * not damaging the full file.
     * @param file {@link} File to split
     * @return {@link Result} storing both halves of the given file
     */
    public static Result<File, File> splitFile(File file) {
        Result<String, String> result = getFileNameAndExtension(file.getName());
        String name1 = result.getResult1() + "_1." + result.getResult2();
        String name2 = result.getResult1() + "_2." + result.getResult2();

        return splitFile(file, name1, name2);
    }


    /**
     * Given a certain file, splits it in half and returns it as two new Files,
     * not damaging the full file.
     * @param file {@link File} to split
     * @param firstName Name of the first half
     * @param secondName Name of the second half
     * @return {@link Result} storing both halves of the given file, named
     * as specified by parameters
     */
    public static Result<File, File> splitFile(File file, String firstName, String secondName) {
        final String auxPath = "C:\\Users\\gmiga\\Documents\\RaidTesting\\Auxiliar\\";
        long fileLength = file.length();

        File file1 = new File(auxPath + firstName);
        File file2 = new File(auxPath + secondName);

        Result<File, File> result = null;

        BufferedInputStream bis = null;
        BufferedOutputStream bos1 = null;
        BufferedOutputStream bos2 = null;

        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            bos1 = new BufferedOutputStream(new FileOutputStream(file1));
            bos2 = new BufferedOutputStream(new FileOutputStream(file2));

            byte[] buffer = new byte[1];

            for (long i = 0; i < fileLength / 2; i++) {
                bis.read(buffer);
                bos1.write(buffer);
            }
            for (long i = (fileLength / 2); i <= fileLength ; i++) {
                bis.read(buffer);
                bos2.write(buffer);
            }

            result = new Result<>(file1, file2);
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
        finally {
            Util.closeResource(bis);
            Util.closeResource(bos1);
            Util.closeResource(bos2);
        }

        return result;
    }


    /**
     * Returns a Result object containing the file name (without the extension)
     * and the file extension as two separate parts.
     *
     * @param file the File object
     * @return a Result<String, String> where result1 is the file name and result2 is the file extension
     */
    public static Result<String, String> getFileNameAndExtension(File file) {
        return getFileNameAndExtension(file.getName());
    }


    /**
     * Returns a Result object containing the file name (without the extension)
     * and the file extension as two separate parts.
     *
     * @param fileName Name of a File
     * @return a Result<String, String> where result1 is the file name and result2 is the file extension
     */
    public static Result<String, String> getFileNameAndExtension(String fileName) {
        int lastIndexOfDot = fileName.lastIndexOf('.');

        // If no extension is found or the dot is the last character
        if (lastIndexOfDot == -1 || lastIndexOfDot == fileName.length() - 1) {
            return new Result<>(fileName, ""); // No extension
        }

        String nameWithoutExtension = fileName.substring(0, lastIndexOfDot);
        String extension = fileName.substring(lastIndexOfDot + 1);

        return new Result<>(nameWithoutExtension, extension);
    }


    /**
     * Method that sleeps until the rest of the servers are up. A
     * message would be sent to the current {@link Server} if they
     * are not up.
     */
    protected void waitForConnections() {
        while (!connectionTestLeft.isConnectionAvailable() || !connectionTestRight.isConnectionAvailable()) {
            System.out.println("PERIPHERAL SERVERS ARE NOT UP");
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
