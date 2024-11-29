package raid.servers;

import raid.threads.ClientManagerThread;
import raid.servers.files.Strategy;
import raid.threads.testers.HearingThread;
import raid.threads.localCommunication.LocalHearerThread;

import static raid.misc.Util.closeResource;
import static raid.misc.Util.getProperty;

import java.io.*;
import java.net.ServerSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;


/**
 * <p>
 * Abstraction of a Server, that's meant to run and retrieve clients'
 * commands related to {@link File} objects: storing, retrieving or deleting
 * them from the memory of the current Server, in a concurrent way. It uses
 * a {@link ClientManagerThread} to achieve so.
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

    /**
     * Properties File path of hosts
     */
    public final static String HOSTS = "/hosts.properties";
    /**
     * Properties File path of ports
     */
    public final static String PORTS = "/ports.properties";

    public static final int WEST_TEST_PORT = Integer.parseInt(getProperty("WEST_TEST_PORT", PORTS));
    public static final int CENTRAL_TEST_PORT = Integer.parseInt(getProperty("CENTRAL_TEST_PORT", PORTS));
    public static final int EAST_TEST_PORT = Integer.parseInt(getProperty("EAST_TEST_PORT", PORTS));

    public static final int WEST_CLIENT_PORT = Integer.parseInt(getProperty("WEST_CLIENT_PORT", PORTS));
    public static final int CENTRAL_CLIENT_PORT = Integer.parseInt(getProperty("CENTRAL_CLIENT_PORT", PORTS));
    public static final int EAST_CLIENT_PORT = Integer.parseInt(getProperty("EAST_CLIENT_PORT", PORTS));

    public static final String WEST_HOST = getProperty("WEST_HOST", HOSTS);
    public static final String CENTRAL_HOST = getProperty("CENTRAL_HOST", HOSTS);
    public static final String EAST_HOST = getProperty("EAST_HOST", HOSTS);


    /**
     * Starts up this {@link Server} instance. It makes it wait for clients
     * to access its port -managing their files concurrently-
     * and enables communication between this and other
     * {@code Server} instances. [CURRENTLY NO WAY OF STOPPING IT
     * APART FROM KILLING THE PROCESS]
     */
    public void boot() {
        ServerSocket serverSocket = null;

        LocalHearerThread localCommunication = null;
        ClientManagerThread clientManager = null;
        HearingThread hearingThread = null;

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("| SERVER IS UP |");

            hearingThread = new HearingThread(testPort);
            localCommunication = new LocalHearerThread(localCommunicationPort, strategy);

            hearingThread.start();
            localCommunication.start();

            while (true) {
                try {
                    ScheduledThreadPoolExecutor scheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(512);

                    // Crear lo threads que van a gestionar la llegada de clientes
                    // y las peticiones de parte de otros servidores
                    clientManager = new ClientManagerThread(serverSocket.accept(), strategy);
                    scheduler.execute(clientManager);
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

            // Cerrar el socket del servidor
            closeResource(serverSocket);
        }
    }
}
