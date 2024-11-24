package raid.servers.files;

import static raid.misc.Util.*;
import raid.servers.Server;
import raid.servers.threads.localCommunication.FileRequestSenderThread;
import raid.servers.threads.localCommunication.NameRequestSenderThread;
import raid.servers.threads.localCommunication.RequestSenderThread;
import raid.misc.Result;

import java.io.*;
import java.net.Socket;

public class FullSavingStrategy extends Strategy {
    public FullSavingStrategy(String path) {
        super(path, StrategyType.Central);
    }

    @Override
    public int saveFile(File file) {
        System.out.println("| STARTING TO SAVE " + file.getName() + " |\n");

        Socket westServerSocket = null; Socket eastServerSocket = null;
        checkPathExistence(path); bootConnections(); waitForConnections();

        try {
            westServerSocket = new Socket(Server.WEST_HOST, Strategy.WEST_LOCAL_CONNECTION_PORT);
            eastServerSocket = new Socket(Server.EAST_HOST, Strategy.EAST_LOCAL_CONNECTION_PORT);

            Result<String, String> fileParts = getFileNameAndExtension(file);
            Result<File, File> result = splitFile(
                    file,
                    fileParts.getResult1() + "_1." + fileParts.getResult2(),
                    fileParts.getResult1() + "_2." + fileParts.getResult2()
            );

            RequestSenderThread f1 = new FileRequestSenderThread(westServerSocket, result.getResult1(), SAVE_FILE);
            RequestSenderThread f2 = new FileRequestSenderThread(eastServerSocket, result.getResult2(), SAVE_FILE);

            f1.start();
            f2.start();

            f1.join();
            f2.join();
        }
        catch (IOException | InterruptedException e) {
            return CRITICAL_ERROR;
        }
        finally {
            closeResource(connectionTestLeft);
            closeResource(connectionTestRight);

            closeResource(westServerSocket);
            closeResource(eastServerSocket);
        }
        selfSaveFile(file);

        return FILE_STORED;
    }

    @Override
    public int deleteFile(String fileName) {
        if (!existsFileWithName(path + "\\" + fileName)) {
            return FILE_NOT_FOUND;
        }

        System.out.println("| STARTING TO DELETE " + fileName + " |\n");

        Socket westServerSocket = null; Socket eastServerSocket = null;
        checkPathExistence(path); bootConnections(); waitForConnections();

        int localMessage = selfDeleteFile(fileName);

        // Si no es "FILE_DELETED", significa que este servidor no contiene el archivo
        // que se desea borrar, con lo que se detiene el proceso en ese caso y se continua
        // si solo si el archivo fue borrado
        if (localMessage == FILE_DELETED) {
            try {
                westServerSocket = new Socket(Server.WEST_HOST, Strategy.WEST_LOCAL_CONNECTION_PORT);
                eastServerSocket = new Socket(Server.EAST_HOST, Strategy.EAST_LOCAL_CONNECTION_PORT);

                Result<String, String> result = getFileNameAndExtension(fileName);

                // Parto el nombre para que ambos tengan nombres diferentes
                String fileName1 = result.getResult1() + "_1." + result.getResult2();
                String fileName2 = result.getResult1() + "_2." + result.getResult2();

                RequestSenderThread f1 = new NameRequestSenderThread(westServerSocket, fileName1, DELETE_FILE);
                RequestSenderThread f2 = new NameRequestSenderThread(eastServerSocket, fileName2, DELETE_FILE);

                f1.start();
                f2.start();

                f1.join();
                f2.join();

                // Cambiar mensaje si no se han borrado todos los archivos
                if (f1.getResult() != SUCCESS || f2.getResult() != SUCCESS) {
                    return CRITICAL_ERROR;
                }
            }
            catch (IOException | InterruptedException e) {
                return CRITICAL_ERROR;
            }
            finally {
                closeResource(connectionTestLeft);
                closeResource(connectionTestRight);

                closeResource(westServerSocket);
                closeResource(eastServerSocket);
            }
        }
        else {
            return CRITICAL_ERROR;
        }

        return FILE_DELETED;
    }

    @Override
    public int getFile(String fileName, String clientHost) {
        if (!existsFileWithName(path + "\\" + fileName)) {
            return FILE_NOT_FOUND;
        }

        System.out.println("| STARTING TO GET " + fileName + " to " + clientHost + "|\n");

        Socket westServerSocket = null; Socket eastServerSocket = null;
        checkPathExistence(path); bootConnections(); waitForConnections();

        try {
            westServerSocket = new Socket(Server.WEST_HOST, WEST_LOCAL_CONNECTION_PORT);
            eastServerSocket = new Socket(Server.EAST_HOST, EAST_LOCAL_CONNECTION_PORT);

            int port1 = Integer.parseInt(getProperty("CLIENT_HEAR_PORT1", Server.PORTS));
            int port2 = Integer.parseInt(getProperty("CLIENT_HEAR_PORT2", Server.PORTS));

            Result<String, String> fileParts = getFileNameAndExtension(fileName);
            String fileName1 = fileParts.getResult1() + "_1." + fileParts.getResult2();
            String fileName2 = fileParts.getResult1() + "_2." + fileParts.getResult2();

            RequestSenderThread f1 = new NameRequestSenderThread(westServerSocket, fileName1, GET_FILE, clientHost, port1);
            RequestSenderThread f2 = new NameRequestSenderThread(eastServerSocket, fileName2, GET_FILE, clientHost, port2);

            f1.start();
            f2.start();

            f1.join();
            f2.join();
        }
        catch (IOException | InterruptedException e) {
            return CRITICAL_ERROR;
        }
        finally {
            closeResource(connectionTestLeft);
            closeResource(connectionTestRight);

            closeResource(westServerSocket);
            closeResource(eastServerSocket);
        }

        return FILE_RETRIEVED;
    }
}
