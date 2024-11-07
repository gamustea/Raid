package raid.servers;

import raid.servers.threads.ClientManager;
import raid.servers.files.strategies.Strategy;
import raid.servers.threads.HearingThread;
import raid.servers.threads.LocalCommunication;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class Server {
    protected Strategy strategy;
    protected String host;
    protected int port;
    protected int testPort;
    protected int localCommunicationPort;

    public static final int GET_FILE = 1;
    public static final int SAVE_FILE = 2;
    public static final int DELETE_FILE = 3;
    public static final int CLOSE_CONNECTION = 4;

    public static final int WEST_TEST_PORT = 55550;
    public static final int CENTRAL_TEST_PORT = 55551;
    public static final int EAST_TEST_PORT = 55552;

    public static final int WEST_CLIENT_PORT = 55553;
    public static final int CENTRAL_CLIENT_PORT = 55554;
    public static final int EAST_CLIENT_PORT = 55555;

    public static final String WEST_HOST = "localhost";
    public static final String CENTRAL_HOST = "localhost";
    public static final String EAST_HOST = "localhost";

    public void boot() {
        ServerSocket serverSocket = null;

        LocalCommunication localCommunication = null;
        ClientManager clientManager = null;
        HearingThread hearingThread = null;

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("| SERVER IS UP |");

            hearingThread = new HearingThread(testPort);
            localCommunication = new LocalCommunication(localCommunicationPort, strategy);

            hearingThread.start();
            localCommunication.start();

            while (true) {
                try {
                    // Crear lo threads que van a gestionar la llegada de clientes
                    // y las peticiones de parte de otros servidores
                    clientManager = new ClientManager(serverSocket.accept(), strategy);
                    clientManager.start();
                }
                catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
        finally {

            // Matar todos los procesos
            if (localCommunication != null && localCommunication.isAlive()) {
                localCommunication.interrupt();
            }
            if (clientManager != null && clientManager.isAlive()) {
                clientManager.interrupt();
            }
            if (hearingThread != null && hearingThread.isAlive()) {
                hearingThread.interrupt();
            }

            // Cerrar los recursos
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                }
                catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }
}
