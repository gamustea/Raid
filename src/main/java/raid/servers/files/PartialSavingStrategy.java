package raid.servers.files;

import raid.servers.Server;
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

public class PartialSavingStrategy extends Strategy {
    public PartialSavingStrategy(String path, StrategyType strategyType) {
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
                fileParts.getResult1() + "_1." + fileParts.getResult2(),
                fileParts.getResult1() + "_2." + fileParts.getResult2()
        );
        try {
            centralServerSocket = new Socket(Server.CENTRAL_HOST, CENTRAL_LOCAL_CONNECTION_PORT);

            File localHalfFile;
            File externalHalfFile;

            // Construye un Socket de formas distintas en función del tipo
            // de estrategia elegida en la construcción
            if (strategyType.equals(East)) {
                peripheralServerSocket = new Socket(Server.WEST_HOST, WEST_LOCAL_CONNECTION_PORT);
                externalHalfFile = result.getResult1();
                localHalfFile = result.getResult2();
            }
            else {
                peripheralServerSocket = new Socket(Server.EAST_HOST, EAST_LOCAL_CONNECTION_PORT);
                externalHalfFile = result.getResult2();
                localHalfFile = result.getResult1();
            }

            f1 = new RequestSenderThread(centralServerSocket, SAVE_FILE, file);
            f2 = new RequestSenderThread(peripheralServerSocket, SAVE_FILE, externalHalfFile);

            f1.start();
            f2.start();

            f1.join();
            f2.join();

            assert localHalfFile != null;
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
        }

        return CRITICAL_ERROR;
    }

    @Override
    public int deleteFile(String fileName) {
        if (!existsFileWithName(fileName)) {
            return FILE_NOT_FOUND;
        }

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
            centralServerSocket = new Socket(Server.CENTRAL_HOST, CENTRAL_LOCAL_CONNECTION_PORT);

            // Construye un Socket de formas distintas en función del tipo
            // de estrategia elegida en la construcción
            Result<String, String> fileParts = getFileNameAndExtension(fileName);
            if (strategyType.equals(East)) {
                peripheralServerSocket = new Socket(Server.WEST_HOST, WEST_LOCAL_CONNECTION_PORT);
                externalHalfFile = fileParts.getResult1() + "_1." + fileParts.getResult2();
                localHalfFile = fileParts.getResult1() + "_2." + fileParts.getResult2();
            }
            else {
                peripheralServerSocket = new Socket(Server.EAST_HOST, EAST_LOCAL_CONNECTION_PORT);
                externalHalfFile = fileParts.getResult1() + "_2." + fileParts.getResult2();
                localHalfFile = fileParts.getResult1() + "_1." + fileParts.getResult2();
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
                sideServerSocket = new Socket(Server.EAST_HOST, EAST_LOCAL_CONNECTION_PORT);
            }
            else {
                sideServerSocket = new Socket(Server.WEST_HOST, WEST_LOCAL_CONNECTION_PORT);
            }

            int port1 = Integer.parseInt(getProperty("CLIENT_HEAR_PORT1", Server.PORTS));
            int port2 = Integer.parseInt(getProperty("CLIENT_HEAR_PORT2", Server.PORTS));

            Result<String, String> fileParts = getFileNameAndExtension(fileName);
            String hereFileName;
            String thereFileName;

            if (strategyType == East) {
                thereFileName = fileParts.getResult1() + "_1." + fileParts.getResult2();
                hereFileName = fileParts.getResult1() + "_2." + fileParts.getResult2();
            }
            else {
                hereFileName = fileParts.getResult1() + "_1." + fileParts.getResult2();
                thereFileName = fileParts.getResult1() + "_2." + fileParts.getResult2();
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
