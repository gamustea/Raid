package raid.servers.files;

import raid.misc.Result;
import raid.misc.Util;
import raid.servers.CentralServer;
import raid.servers.EastServer;
import raid.servers.Server;
import raid.servers.WestServer;
import raid.threads.testers.ConnectionTestThread;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

import static raid.misc.Util.*;


public abstract class ProcessingStrategy {
    protected ConnectionTestThread connectionTestLeft;
    protected ConnectionTestThread connectionTestRight;
    protected CyclicBarrier barrier;
    protected Path path;
    protected StrategyType strategyType;

    protected final static int WEST_LOCAL_CONNECTION_PORT = Integer.parseInt(getProperty("WEST_LOCAL_CONNECTION_PORT", PORTS));
    protected final static int CENTRAL_LOCAL_CONNECTION_PORT = Integer.parseInt(getProperty("CENTRAL_LOCAL_CONNECTION_PORT", PORTS));
    protected final static int EAST_LOCAL_CONNECTION_PORT = Integer.parseInt(getProperty("EAST_LOCAL_CONNECTION_PORT", PORTS));

    protected ProcessingStrategy(String pathName, StrategyType strategyType) {
        path = Path.of(pathName);
        barrier = new CyclicBarrier(3);
        checkPathExistence(path);
        selectLocalConnections(strategyType);
    }


    /**
     * Performs the saving processing based on the current {@link ProcessingStrategy}
     * instance. At the end of the call, the current {@link Server} and the rest of them
     * would contain a full copy of the file (if it's a {@link CentralServer}) or half of it
     * (if it's {@link WestServer} or a {@link EastServer}).
     * @param file File to store
     * @return {@link Util} code representing success or failure
     */
    public abstract int saveFile(File file);


    /**
     * Performs the deleting processing based on the current {@link ProcessingStrategy}
     * instance. At the end of the call, the current {@link Server} and the rest of them
     * would not contain the given file.
     * @param file Name of the file to delete
     * @return {@link Util} code representing success or failure
     */
    public abstract int deleteFile(String file);


    /**
     * Performs the getting processing based on the current {@link ProcessingStrategy}
     * instance. At the end of the call, the current {@link Server} and/or the rest of them
     * would have retrieved to the client the halves of the file he/she had requested.
     * @param file Name of the file to retrieve
     * @return {@link Util} code representing success or failure
     */
    public abstract int getFile(String file, String clientHost);


    /**
     * Gets a list of the files that the host {@link Server} contains.
     * @return {@link Result} containing a success or failure code form
     * {@link Util} and a list (might be empty) of the listed files
     */
    public Result<Integer, List<String>> listFiles() {
        File directory = new File(String.valueOf(path.toFile()));
        List<String> fileNames = new ArrayList<>();
        for (File file : Objects.requireNonNull(directory.listFiles())) {
            fileNames.add(getCorrectFileName(file.getName()));
        }
        return new Result<>(FILES_LISTED, fileNames);
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
            closeResource(bis);
            closeResource(bos);
        }

        return FILE_STORED;
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


    /**
     * Method that allows a {@link Server} instance to get a given
     * {@link File} from its storaging path.
     *
     * @param fileName Name of the File to search
     * @param clientHost Address of client asking for the File
     * @param port Port number of the client
     * @return Error code from {@link Util}
     */
    public int selfGetFile(String fileName, String clientHost, int port) {
        Socket socket = null;
        int message;
        ObjectOutputStream strategyOut = null;

        try {
            CyclicBarrier barrier = new CyclicBarrier(2);
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            ConnectionTestThread connectionTest = new ConnectionTestThread(60002, clientHost, barrier);

            scheduler.scheduleAtFixedRate(connectionTest, 0, 1, TimeUnit.SECONDS);
            barrier.await();

            socket = new Socket(clientHost, port);
            strategyOut = new ObjectOutputStream(socket.getOutputStream());

            File fileToRetrieve = new File(path + "\\" + fileName);
            checkPathExistence(fileToRetrieve.toPath());
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
        catch (IOException | InterruptedException | BrokenBarrierException e) {
            message = CRITICAL_ERROR;
        } finally {
            closeResource(socket);
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
        // Crear un Scheduler para ejecutar una operación cada cierto tiempo
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

        scheduler.scheduleAtFixedRate(connectionTestLeft, 0, 2, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(connectionTestRight, 0, 2, TimeUnit.SECONDS);

        if (connectionTestLeft.isConnectionDown() || connectionTestRight.isConnectionDown()) {
            System.out.println("PERIPHERAL SERVERS ARE NOT UP");
        }
        try {
            barrier.await();
        }
        catch (BrokenBarrierException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            shutdownExecutorService(scheduler, 0, TimeUnit.SECONDS);
        }
    }


    /**
     * Method that distributes the communications based on the {@link StrategyType}
     * given. F.E.: if the current {@link Server} instance is a {@link WestServer},
     * this method assigns the {@link ConnectionTestThread} instances to another
     * {@link EastServer} and a {@link CentralServer}.
     * @param strategyType Type of instance of the server.
     */
    private void selectLocalConnections(StrategyType strategyType) {
        this.strategyType = strategyType;

        switch (strategyType) {
            case Central: {
                this.connectionTestLeft = new ConnectionTestThread(WEST_TEST_PORT, WEST_HOST, barrier);
                this.connectionTestRight = new ConnectionTestThread(EAST_TEST_PORT, EAST_HOST, barrier);
                break;
            }
            case East: {
                this.connectionTestLeft = new ConnectionTestThread(WEST_TEST_PORT, WEST_HOST, barrier);
                this.connectionTestRight = new ConnectionTestThread(CENTRAL_TEST_PORT, CENTRAL_HOST, barrier);
            }
            case West: {
                this.connectionTestLeft = new ConnectionTestThread(CENTRAL_TEST_PORT, CENTRAL_HOST, barrier);
                this.connectionTestRight = new ConnectionTestThread(EAST_TEST_PORT, EAST_HOST, barrier);
                break;
            }
        }
    }
}
