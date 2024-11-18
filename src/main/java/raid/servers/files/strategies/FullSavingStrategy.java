package raid.servers.files.strategies;

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
    public String saveFile(File file) {
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

            RequestSenderThread f1 = new FileRequestSenderThread(westServerSocket, result.getResult1(), Server.SAVE_FILE);
            RequestSenderThread f2 = new FileRequestSenderThread(eastServerSocket, result.getResult2(), Server.SAVE_FILE);

            f1.start();
            f2.start();

            f1.join();
            f2.join();

            System.out.println(f1.getResult());
            System.out.println(f2.getResult());
        }
        catch (IOException | InterruptedException e) {
            return "| ERROR WHILE STORAGING |";
        }
        finally {
            Server.closeResource(connectionTestLeft);
            Server.closeResource(connectionTestRight);

            Server.closeResource(westServerSocket);
            Server.closeResource(eastServerSocket);
        }
        selfSaveFile(file);

        return "| FILE SAVED |";
    }

    @Override
    public String deleteFile(String fileName) {
        System.out.println("| STARTING TO DELETE " + fileName + " |\n");

        Socket westServerSocket = null; Socket eastServerSocket = null;
        checkPathExistence(path); bootConnections(); waitForConnections();

        String finalMessage = selfDeleteFile(fileName);

        if (!finalMessage.equals("| ERROR FILE NOT FOUND |")) {
            try {
                westServerSocket = new Socket(Server.WEST_HOST, Strategy.WEST_LOCAL_CONNECTION_PORT);
                eastServerSocket = new Socket(Server.EAST_HOST, Strategy.EAST_LOCAL_CONNECTION_PORT);

                Result<String, String> result = getFileNameAndExtension(fileName);

                String fileName1 = result.getResult1() + "_1." + result.getResult2();
                String fileName2 = result.getResult1() + "_2." + result.getResult2();

                RequestSenderThread f1 = new NameRequestSenderThread(westServerSocket, fileName1, Server.DELETE_FILE);
                RequestSenderThread f2 = new NameRequestSenderThread(eastServerSocket, fileName2, Server.DELETE_FILE);

                f1.start();
                f2.start();

                f1.join();
                f2.join();

                System.out.println(f1.getResult());
                System.out.println(f2.getResult());

                if (f1.getResult().equalsIgnoreCase("| FILE SUCCESSFULLY DELETED |")
                    && f2.getResult().equalsIgnoreCase("| FILE SUCCESSFULLY DELETED |")) {
                    finalMessage = "| FILE COMPLETELY DELETED |";
                }
                else {
                    finalMessage = "| FILE NOT ENTIRELY DELETED |";
                }
            }
            catch (IOException | InterruptedException e) {
                return "| ERROR WHILE DELETING |";
            }
            finally {
                Server.closeResource(connectionTestLeft);
                Server.closeResource(connectionTestRight);

                Server.closeResource(westServerSocket);
                Server.closeResource(eastServerSocket);
            }
        }

        return finalMessage;
    }

    @Override
    public String getFile(String file) {
        return null;
    }
}
