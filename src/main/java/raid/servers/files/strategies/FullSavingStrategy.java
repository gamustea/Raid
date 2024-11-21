package raid.servers.files.strategies;

import raid.RS;
import raid.servers.Server;
import raid.threads.localCommunication.FileRequestSenderThread;
import raid.threads.localCommunication.NameRequestSenderThread;
import raid.threads.localCommunication.RequestSenderThread;
import returning.Result;

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

            RequestSenderThread f1 = new FileRequestSenderThread(westServerSocket, result.getResult1(), RS.SAVE_FILE);
            RequestSenderThread f2 = new FileRequestSenderThread(eastServerSocket, result.getResult2(), RS.SAVE_FILE);

            f1.start();
            f2.start();

            f1.join();
            f2.join();
        }
        catch (IOException | InterruptedException e) {
            return RS.CRITICAL_ERROR;
        }
        finally {
            Server.closeResource(connectionTestLeft);
            Server.closeResource(connectionTestRight);

            Server.closeResource(westServerSocket);
            Server.closeResource(eastServerSocket);
        }
        selfSaveFile(file);

        return RS.FILE_STORED;
    }

    @Override
    public int deleteFile(String fileName) {
        System.out.println("| STARTING TO DELETE " + fileName + " |\n");

        Socket westServerSocket = null; Socket eastServerSocket = null;
        checkPathExistence(path); bootConnections(); waitForConnections();

        int localMessage = selfDeleteFile(fileName);

        if (localMessage == RS.FILE_DELETED) {
            try {
                westServerSocket = new Socket(Server.WEST_HOST, Strategy.WEST_LOCAL_CONNECTION_PORT);
                eastServerSocket = new Socket(Server.EAST_HOST, Strategy.EAST_LOCAL_CONNECTION_PORT);

                Result<String, String> result = getFileNameAndExtension(fileName);

                String fileName1 = result.getResult1() + "_1." + result.getResult2();
                String fileName2 = result.getResult1() + "_2." + result.getResult2();

                RequestSenderThread f1 = new NameRequestSenderThread(westServerSocket, fileName1, RS.DELETE_FILE);
                RequestSenderThread f2 = new NameRequestSenderThread(eastServerSocket, fileName2, RS.DELETE_FILE);

                f1.start();
                f2.start();

                f1.join();
                f2.join();

                // Cambiar mensaje si no se han borrado todos los archivos
                if (!(f1.getResult() == RS.SUCCESS) || !(f2.getResult() == RS.SUCCESS)) {
                    return RS.CRITICAL_ERROR;
                }

                System.out.println(f1.getResult());
                System.out.println(f2.getResult());
            }
            catch (IOException | InterruptedException e) {
                return RS.CRITICAL_ERROR;
            }
            finally {
                Server.closeResource(connectionTestLeft);
                Server.closeResource(connectionTestRight);

                Server.closeResource(westServerSocket);
                Server.closeResource(eastServerSocket);
            }
        }
        else {
            return RS.CRITICAL_ERROR;
        }

        return RS.FILE_DELETED;
    }

    @Override
    public int getFile(String file, String host) {
        return 0;
    }
}
