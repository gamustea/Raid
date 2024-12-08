package raid.servers.files;

import static raid.misc.Util.*;

import raid.servers.CentralServer;
import raid.servers.EastServer;
import raid.servers.Server;
import raid.servers.WestServer;
import raid.threads.localCommunication.RequestSenderThread;
import raid.misc.Result;

import java.io.*;
import java.net.Socket;


/**
 * Intance of {@link ProcessingStrategy} that allows a {@link CentralServer} instance of a
 * {@link Server} to perform full saving operations, so that the files given to this
 * instanced strategy would save, retrieve and delete full content of {@link File} objets.
 * It communicates with the peripheral servers ({@link EastServer} and {@link WestServer})
 * to perform the complete process.
 */
public class FullProcessingStrategy extends ProcessingStrategy {

    /**
     * Builds a full processing strategy instance for a {@link CentralServer} instance.
     * @param path Path in which the host server would proccess the data given.
     */
    public FullProcessingStrategy(String path) {
        super(path, StrategyType.Central);
    }


    @Override
    public int saveFile(File file) {
        System.out.println("| STARTING TO SAVE " + file.getName() + " |\n");
        selfSaveFile(file);

        Socket westServerSocket = null;
        Socket eastServerSocket = null;

        checkPathExistence(path);
        waitForConnections();

        try {
            westServerSocket = new Socket(WEST_HOST, ProcessingStrategy.WEST_LOCAL_CONNECTION_PORT);
            eastServerSocket = new Socket(EAST_HOST, ProcessingStrategy.EAST_LOCAL_CONNECTION_PORT);

            Result<String, String> fileParts = getFileNameAndExtension(file);
            Result<File, File> result = splitFile(
                    file,
                    path + "\\Auxiliary",
                    fileParts.result1() + "_1." + fileParts.result2(),
                    fileParts.result1() + "_2." + fileParts.result2()
            );

            RequestSenderThread f1 = new RequestSenderThread(westServerSocket, SAVE_FILE, result.result1());
            RequestSenderThread f2 = new RequestSenderThread(eastServerSocket, SAVE_FILE, result.result2());

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

            deleteDirectory(new File(path + "\\Auxiliary"));
        }

        return FILE_STORED;
    }


    @Override
    public int deleteFile(String fileName) {
        if (!existsFileWithName(path + "\\" + fileName)) {
            return FILE_NOT_FOUND;
        }

        System.out.println("| STARTING TO DELETE " + fileName + " |\n");

        Socket westServerSocket = null;
        Socket eastServerSocket = null;
        checkPathExistence(path);

        int localMessage = selfDeleteFile(fileName);

        // Si no es "FILE_DELETED", significa que este servidor no contiene el archivo
        // que se desea borrar, con lo que se detiene el proceso en ese caso y se continua
        // si solo si el archivo fue borrado
        if (localMessage == FILE_DELETED) {
            try {
                westServerSocket = new Socket(WEST_HOST, ProcessingStrategy.WEST_LOCAL_CONNECTION_PORT);
                eastServerSocket = new Socket(EAST_HOST, ProcessingStrategy.EAST_LOCAL_CONNECTION_PORT);

                Result<String, String> result = getFileNameAndExtension(fileName);

                // Parto el nombre para que ambos tengan nombres diferentes
                String fileName1 = result.result1() + "_1." + result.result2();
                String fileName2 = result.result1() + "_2." + result.result2();

                RequestSenderThread f1 = new RequestSenderThread(westServerSocket, DELETE_FILE, fileName1);
                RequestSenderThread f2 = new RequestSenderThread(eastServerSocket, DELETE_FILE, fileName2);

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

        Socket westServerSocket = null;
        Socket eastServerSocket = null;
        checkPathExistence(path);
        waitForConnections();

        try {
            westServerSocket = new Socket(WEST_HOST, WEST_LOCAL_CONNECTION_PORT);
            eastServerSocket = new Socket(EAST_HOST, EAST_LOCAL_CONNECTION_PORT);

            int port1 = Integer.parseInt(getProperty("CLIENT_HEAR_PORT1", PORTS));
            int port2 = Integer.parseInt(getProperty("CLIENT_HEAR_PORT2", PORTS));

            Result<String, String> fileParts = getFileNameAndExtension(fileName);
            String fileName1 = fileParts.result1() + "_1." + fileParts.result2();
            String fileName2 = fileParts.result1() + "_2." + fileParts.result2();

            RequestSenderThread f1 = new RequestSenderThread(westServerSocket, GET_FILE, fileName1,  clientHost, port1);
            RequestSenderThread f2 = new RequestSenderThread(eastServerSocket, GET_FILE, fileName2,  clientHost, port2);

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

        return SUCCESS;
    }
}
