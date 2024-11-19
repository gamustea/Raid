package raid.servers.files.strategies;

import raid.RS;
import raid.servers.Server;
import raid.servers.WestServer;
import raid.threads.testers.ConnectionTestThread;
import returning.Result;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static java.lang.Thread.sleep;

public abstract class Strategy {
    protected ConnectionTestThread connectionTestLeft;
    protected ConnectionTestThread connectionTestRight;
    protected Path path;
    protected StrategyType strategyType;

    protected final static int WEST_LOCAL_CONNECTION_PORT = Integer.parseInt(getProperty("WEST_LOCAL_CONNECTION_PORT"));
    protected final static int CENTRAL_LOCAL_CONNECTION_PORT = Integer.parseInt(getProperty("CENTRAL_LOCAL_CONNECTION_PORT"));
    protected final static int EAST_LOCAL_CONNECTION_PORT = Integer.parseInt(getProperty("EAST_LOCAL_CONNECTION_PORT"));

    public abstract int saveFile(File file);
    public abstract int deleteFile(String file);
    public abstract int getFile(String file);

    protected Strategy(String pathName, StrategyType strategyType) {
        path = Path.of(pathName);
        checkPathExistence(path);

        this.strategyType = strategyType;

        switch (strategyType) {
            case StrategyType.Central: {
                this.connectionTestLeft = new ConnectionTestThread(Server.WEST_TEST_PORT, Server.WEST_HOST);
                this.connectionTestRight = new ConnectionTestThread(Server.EAST_TEST_PORT, Server.EAST_HOST);
                break;
            }
            case StrategyType.East: {
                this.connectionTestLeft = new ConnectionTestThread(Server.WEST_TEST_PORT, Server.WEST_HOST);
                this.connectionTestRight = new ConnectionTestThread(WestServer.CENTRAL_TEST_PORT, Server.CENTRAL_HOST);
            }
            case StrategyType.West: {
                this.connectionTestLeft = new ConnectionTestThread(WestServer.CENTRAL_TEST_PORT, Server.CENTRAL_HOST);
                this.connectionTestRight = new ConnectionTestThread(Server.EAST_TEST_PORT, Server.EAST_HOST);
                break;
            }
        }
    }


    private static String getProperty(String clave) {
        String valor = null;
        try {
            Properties props = new Properties();
            InputStream prIS = Server.class.getResourceAsStream("/ports.properties");
            props.load(prIS);
            valor = props.getProperty(clave);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        return valor;
    }


    /**
     * Method that allows a {@link Server} instance to save a given {@link File}
     * in its file storaging path.
     * @param file {@link File} to store
     * @return "SAVED" if the file was successfully stored
     */
    public int selfSaveFile(File file) {
        File storedFile = new File(path +
                "\\" +
                file.getName());
        byte[] buffer = new byte[(int) (file.length() - 1)];

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;

        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            bos = new BufferedOutputStream(new FileOutputStream(storedFile));

            if (bis.read(buffer) != -1) {
                bos.write(buffer);
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        finally {
            Server.closeResource(bis);
            Server.closeResource(bos);
        }

        return RS.FILE_STORED;
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
                return RS.FILE_DELETED;
            }
            return RS.CRITICAL_ERROR;
        }

        return RS.FILE_NOT_FOUND;
    }


    public int selfGetFile(String file) {
        return 0;
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
            e.printStackTrace();
        }
        finally {
            Server.closeResource(bis);
            Server.closeResource(bos1);
            Server.closeResource(bos2);
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


    /**
     * Checks that the given path exists in the current host. If it does not,
     * method creates it in the given.
     * @param path {@link Path} to check
     */
    protected static void checkPathExistence(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }
        } catch (IOException e) {
            System.out.println("Error while creating " + path);
        }
    }
}
