package raid.servers.files.strategies;

import raid.servers.Server;
import raid.servers.WestServer;
import raid.servers.threads.ConnectionTest;
import returning.Result;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.Thread.sleep;

public abstract class Strategy {
    protected ConnectionTest connectionTestLeft;
    protected ConnectionTest connectionTestRight;
    protected Path path;

    protected final static int WEST_LOCAL_CONNECTION_PORT = 55556;
    protected final static int CENTRAL_LOCAL_CONNECTION_PORT = 55557;
    protected final static int EAST_LOCAL_CONNECTION_PORT = 55558;

    public abstract String saveFile(File file);
    public abstract String deleteFile(String file);
    public abstract String getFile(String file, Socket clientSocket);

    protected Strategy(String pathName, StrategyType strategyType) {
        this.path = Path.of(pathName);
        switch (strategyType) {
            case StrategyType.Central: {
                this.connectionTestLeft = new ConnectionTest(Server.WEST_TEST_PORT, Server.WEST_HOST);
                this.connectionTestRight = new ConnectionTest(Server.EAST_TEST_PORT, Server.EAST_HOST);
                break;
            }
            case StrategyType.East: {
                this.connectionTestLeft = new ConnectionTest(Server.WEST_TEST_PORT, Server.WEST_HOST);
                this.connectionTestRight = new ConnectionTest(WestServer.CENTRAL_TEST_PORT, Server.CENTRAL_HOST);
            }
            case StrategyType.West: {
                this.connectionTestLeft = new ConnectionTest(WestServer.CENTRAL_TEST_PORT, Server.CENTRAL_HOST);
                this.connectionTestRight = new ConnectionTest(Server.EAST_TEST_PORT, Server.EAST_HOST);
                break;
            }
        }

        try {
            Files.createDirectory(Paths.get(pathName));
        }
        catch (IOException e) {
            e.getMessage();
        }
    }

    public String selfSaveFile(File file) {
        File storedFile = new File(path + "\\" + file.getName());
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
            if (bis != null) {
                try {
                    bis.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return "SAVED";
    }

    public String selfDeleteFile(String fileName) {
        File fileToDelete = new File(fileName);
        if (fileToDelete.exists()) {
            fileToDelete.delete();
        }
        else {
            return "| ERROR FILE DOES NOT EXIST |";
        }

        return "| FILE SUCCESSFULLY DELETED |";
    }

    public File selfGetFile(String file, Socket clientSocket) {
        return null;
    }

    public static Result<File, File> splitFile(File file) {
        return splitFile(file, "File1", "File2");
    }

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
            if (bis != null) {
                try {
                    bis.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bos1 != null) {
                try {
                    bos1.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bos2 != null) {
                try {
                    bos2.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    protected void waitForConnection() {
        while (!connectionTestLeft.isConnectionAvailable() || !connectionTestRight.isConnectionAvailable()) {
            System.out.println("PERIPHERAL SERVERS ARE NOT UP");
            try {
                sleep(500);
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected void bootConnections() {
        if (!connectionTestLeft.isAlive()) {
            connectionTestLeft.start();
        }
        if (!connectionTestRight.isAlive()) {
            connectionTestRight.start();
        }
    }


}
