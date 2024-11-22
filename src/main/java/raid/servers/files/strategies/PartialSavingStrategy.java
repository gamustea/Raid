package raid.servers.files.strategies;

import raid.servers.Server;
import raid.threads.localCommunication.FileRequestSenderThread;
import raid.threads.localCommunication.NameRequestSenderThread;
import raid.threads.localCommunication.RequestSenderThread;
import returning.Result;

import static raid.Util.*;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

import static raid.Util.FILE_NOT_FOUND;
import static raid.Util.existsFileWithName;
import static raid.servers.files.strategies.StrategyType.East;

public class PartialSavingStrategy extends Strategy {
    public PartialSavingStrategy(String path, StrategyType strategyType) {
        super(path, strategyType);
    }

    @Override
    public int saveFile(File file) {
        Socket centralServerSocket = null; Socket peripheralServerSocket = null;
        RequestSenderThread f1 = null; RequestSenderThread f2 = null;

        checkPathExistence(path); bootConnections(); waitForConnections();

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

            f1 = new FileRequestSenderThread(centralServerSocket, file, SAVE_FILE);
            f2 = new FileRequestSenderThread(peripheralServerSocket, externalHalfFile, SAVE_FILE);

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

        Socket centralServerSocket = null; Socket peripheralServerSocket = null;
        RequestSenderThread f1 = null; RequestSenderThread f2 = null;

        String localHalfFile = null;
        String externalHalfFile;

        checkPathExistence(path); bootConnections(); waitForConnections();

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
            f1 = new NameRequestSenderThread(centralServerSocket, fileName, DELETE_FILE);
            f2 = new NameRequestSenderThread(peripheralServerSocket, externalHalfFile, DELETE_FILE);

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

        return 0;
    }
}
