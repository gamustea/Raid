package raid.servers;

import raid.servers.threads.ClientManager;
import raid.servers.files.strategies.Strategy;
import raid.servers.threads.HearingThread;
import raid.servers.threads.LocalCommunication;

import java.io.*;
import java.net.ServerSocket;

/**
 * <p>
 * Abstraction of a Server, that's meant to run and retrieve clients'
 * commands related to {@link File} objects: storing, storaging or deleting
 * them from the memory of the current Server, in a concurrent way. It uses
 * a {@link ClientManager} to achieve so.
 * This server can be built in three different ways:
 * <ul>
 *     <li>{@link CentralServer}</li>
 *     <li>{@link EastServer}</li>
 *     <li>{@link WestServer}</li>
 * </ul>
 * </p>
 * <p>Any of the previously mentioned instances can manage {@link File}
 * objects (if only there is one of each instance up). Communication among
 * them is automatically established thanks to a {@link HearingThread}.
 * </p>
 */
public abstract class Server {

    /**
     * {@link Strategy} instance of method to managing files
     */
    protected Strategy strategy;

    /**
     * IP of the server, represented by a {@link String}
     */
    protected String host;

    /**
     * Public port communication between this {@code Server}
     * and the potential client hosts
     */
    protected int port;

    /**
     * Private port used by other {@link Server} instances to check whether
     * THIS instance of {@code Server} is up or not.
     */
    protected int testPort;

    /**
     * Private port used by other {@link Server} instances to communicate with
     * THIS {@code Server} to retrieve commands;
     */
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

    /**
     * Starts up this {@link Server} instance. It makes it wait for clients
     * to access its port -managing their files concurrently-
     * and enables communication between this and other
     * {@code Server} instances. [CURRENTLY NO WAY OF STOPPING IT
     * APART FROM KILLING THE PROCESS]
     */
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
            closeResource(localCommunication);
            closeResource(clientManager);
            closeResource(hearingThread);

            closeResource(serverSocket);
        }
    }

    public static void closeResource(Thread thread) {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

    public static void closeResource(Closeable closableResource) {
        if (closableResource != null) {
            try {
                closableResource.close();
            } catch (IOException e) {
                System.out.println("| ERROR WHILE CLOSING RESOURCE " + closableResource + "|");
            }
        }
    }
}
