package raid.servers.files.strategies;

import raid.servers.Server;
import raid.threads.localCommunication.FileRequestSenderThread;
import raid.threads.localCommunication.NameRequestSenderThread;
import raid.threads.localCommunication.RequestSenderThread;
import returning.Result;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

import static raid.servers.files.strategies.StrategyType.East;

public class PartialSavingStrategy extends Strategy {
    public PartialSavingStrategy(String path, StrategyType strategyType) {
        super(path, strategyType);
    }

    @Override
    public String saveFile(File file) {
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

            f1 = new FileRequestSenderThread(centralServerSocket, file, Server.SAVE_FILE);
            f2 = new FileRequestSenderThread(peripheralServerSocket, externalHalfFile, Server.SAVE_FILE);

            f1.start();
            f2.start();
            f1.join();
            f2.join();

            System.out.println(f1.getResult());
            System.out.println(f2.getResult());

            assert localHalfFile != null;
            selfSaveFile(localHalfFile);

            return "SAVED";
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            Server.closeResource(centralServerSocket);
            Server.closeResource(peripheralServerSocket);
            Server.closeResource(f1);
            Server.closeResource(f2);
        }

        return "ERROR";
    }

    @Override
    public String deleteFile(String file) {
        Socket centralServerSocket = null; Socket peripheralServerSocket = null;
        RequestSenderThread f1 = null; RequestSenderThread f2 = null;

        String localHalfFile = null;
        String externalHalfFile;

        checkPathExistence(path); bootConnections(); waitForConnections();

        try {
            centralServerSocket = new Socket(Server.CENTRAL_HOST, CENTRAL_LOCAL_CONNECTION_PORT);

            // Construye un Socket de formas distintas en función del tipo
            // de estrategia elegida en la construcción
            Result<String, String> fileParts = getFileNameAndExtension(file);
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
            f1 = new NameRequestSenderThread(centralServerSocket, file, Server.DELETE_FILE);
            f2 = new NameRequestSenderThread(peripheralServerSocket, externalHalfFile, Server.DELETE_FILE);

            f1.start();
            f2.start();
            f1.join();
            f2.join();

            System.out.println(f1.getResult());
            System.out.println(f2.getResult());
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            Server.closeResource(centralServerSocket);
            Server.closeResource(peripheralServerSocket);
            Server.closeResource(f1);
            Server.closeResource(f2);
        }

        // Borro la mitad correspondiente de este servidor
        assert localHalfFile != null;
        selfDeleteFile(localHalfFile);

        return "| FILE COMPLETELY DELETED |";
    }

    @Override
    public String getFile(String file) {
        return null;
    }
}
