package raid.servers.files;

import raid.servers.EastServer;
import raid.servers.Server;
import raid.servers.WestServer;
import raid.threads.localCommunication.RequestSenderThread;
import raid.misc.Result;

import static raid.misc.Util.*;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

import static raid.misc.Util.FILE_NOT_FOUND;
import static raid.misc.Util.existsFileWithName;
import static raid.servers.files.StrategyType.East;
import static raid.servers.files.StrategyType.West;


/**
 * Extension of {@link ProcessingStrategy} that allow {@link Server} instances that are not
 * meant to store full data (such as {@link EastServer} and {@link WestServer}) to
 * perform such operations. During the file processing, this class would make the
 * server hosting this instance to communicate with the other partial saving strategy
 * server and the {@link FullProcessingStrategy} instanced Server.
 */
public class PartialProcessingStrategy extends ProcessingStrategy {

    /**
     * Builds a partial saving strategy instance for a {@link Server}. It
     * requires to determine if it's for a {@link WestServer} or a {@link EastServer},
     * so that the communication is effective.
     * @param path Determines the path in which the current {@link Server} is storing its data
     * @param strategyType {@link StrategyType} among {@code East} and {@code West}
     */
    public PartialProcessingStrategy(String path, StrategyType strategyType) {
        super(path, strategyType);
    }

    @Override
    public int saveFile(File file) {
        System.out.println("| STARTING TO SAVE " + file.getName() + " |\n");

        Socket centralServerSocket = null;
        Socket peripheralServerSocket = null;

        RequestSenderThread f1 = null;
        RequestSenderThread f2 = null;

        checkPathExistence(path);
        waitForConnections();

        Result<String, String> fileParts = getFileNameAndExtension(file);
        Result<File, File> result = splitFile(
                file,
                path + "\\Auxiliary",
                fileParts.result1() + "_1." + fileParts.result2(),
                fileParts.result1() + "_2." + fileParts.result2()
        );
        try {
            centralServerSocket = new Socket(CENTRAL_HOST, CENTRAL_LOCAL_CONNECTION_PORT);

            File localHalfFile;
            File externalHalfFile;

            // Construye un Socket de formas distintas en función del tipo
            // de estrategia elegida en la construcción
            if (strategyType.equals(East)) {
                peripheralServerSocket = new Socket(WEST_HOST, WEST_LOCAL_CONNECTION_PORT);
                externalHalfFile = result.result1();
                localHalfFile = result.result2();
            }
            else {
                peripheralServerSocket = new Socket(EAST_HOST, EAST_LOCAL_CONNECTION_PORT);
                externalHalfFile = result.result2();
                localHalfFile = result.result1();
            }

            f1 = new RequestSenderThread(centralServerSocket, SAVE_FILE, file);
            f2 = new RequestSenderThread(peripheralServerSocket, SAVE_FILE, externalHalfFile);

            f1.start();
            f2.start();

            f1.join();
            f2.join();

            selfSaveFile(localHalfFile);

            return FILE_STORED;
        }
        catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
        finally {
            closeResource(centralServerSocket);
            closeResource(peripheralServerSocket);
            closeResource(f1);
            closeResource(f2);
            deleteDirectory(new File(path + "\\Auxiliary"));
        }

        return CRITICAL_ERROR;
    }

    @Override
    public int deleteFile(String fileName) {
        if (!existsFileWithName(fileName)) {
            return FILE_NOT_FOUND;
        }

        System.out.println("| STARTING TO SAVE " + fileName + " |\n");

        Socket centralServerSocket = null;
        Socket peripheralServerSocket = null;
        RequestSenderThread f1 = null;
        RequestSenderThread f2 = null;

        String localHalfFile = null;
        String externalHalfFile;

        checkPathExistence(path);
        waitForConnections();

        int finalMessage = FILE_DELETED;

        try {
            centralServerSocket = new Socket(CENTRAL_HOST, CENTRAL_LOCAL_CONNECTION_PORT);

            // Construye un Socket de formas distintas en función del tipo
            // de estrategia elegida en la construcción
            Result<String, String> fileParts = getFileNameAndExtension(fileName);
            if (strategyType.equals(East)) {
                peripheralServerSocket = new Socket(WEST_HOST, WEST_LOCAL_CONNECTION_PORT);
                externalHalfFile = fileParts.result1() + "_1." + fileParts.result2();
                localHalfFile = fileParts.result1() + "_2." + fileParts.result2();
            }
            else {
                peripheralServerSocket = new Socket(EAST_HOST, EAST_LOCAL_CONNECTION_PORT);
                externalHalfFile = fileParts.result1() + "_2." + fileParts.result2();
                localHalfFile = fileParts.result1() + "_1." + fileParts.result2();
            }

            // Manda a cada Thread a realizar su correspondiente tarea (en este caso
            // borrar el archivo de cada servidor correspondiente). A CentralServer
            // le envío el nombre entero y al periférico, la mitad correspondiente
            f1 = new RequestSenderThread(centralServerSocket, DELETE_FILE, fileName);
            f2 = new RequestSenderThread(peripheralServerSocket, DELETE_FILE, externalHalfFile);

            f1.start();
            f2.start();
            f1.join();
            f2.join();

            if (!(f1.getResult() == SUCCESS) || !(f2.getResult() == SUCCESS)) {
                finalMessage = CRITICAL_ERROR;
            }
        }
        catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
        finally {
            closeResource(centralServerSocket);
            closeResource(peripheralServerSocket);
            closeResource(f1);
            closeResource(f2);
        }
        selfDeleteFile(localHalfFile);

        return finalMessage;
    }

    @Override
    public int getFile(String fileName, String clientHost) {
        if (!existsFileWithName(fileName)) {
            return FILE_NOT_FOUND;
        }

        System.out.println("| STARTING TO GET " + fileName + " to " + clientHost + "|\n");

        Socket sideServerSocket = null;
        checkPathExistence(path);
        waitForConnections();

        try {
            if (strategyType == West) {
                sideServerSocket = new Socket(EAST_HOST, EAST_LOCAL_CONNECTION_PORT);
            }
            else {
                sideServerSocket = new Socket(WEST_HOST, WEST_LOCAL_CONNECTION_PORT);
            }

            int port1 = Integer.parseInt(getProperty("CLIENT_HEAR_PORT1", PORTS));
            int port2 = Integer.parseInt(getProperty("CLIENT_HEAR_PORT2", PORTS));

            Result<String, String> fileParts = getFileNameAndExtension(fileName);
            String hereFileName;
            String thereFileName;

            if (strategyType == East) {
                thereFileName = fileParts.result1() + "_1." + fileParts.result2();
                hereFileName = fileParts.result1() + "_2." + fileParts.result2();
            }
            else {
                hereFileName = fileParts.result1() + "_1." + fileParts.result2();
                thereFileName = fileParts.result1() + "_2." + fileParts.result2();
            }

            selfGetFile(hereFileName, clientHost, port2);

            RequestSenderThread requestSenderThread = new RequestSenderThread(sideServerSocket, GET_FILE, thereFileName,  clientHost, port1);

            requestSenderThread.start();
            requestSenderThread.join();
        }
        catch (IOException | InterruptedException e) {
            return CRITICAL_ERROR;
        }
        finally {
            closeResource(connectionTestLeft);
            closeResource(connectionTestRight);

            closeResource(sideServerSocket);
        }

        return SUCCESS;
    }
}
