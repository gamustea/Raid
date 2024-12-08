package raid.threads.localCommunication;

import raid.misc.Util;
import raid.servers.Server;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static raid.misc.Util.*;


/**
 * Instance of a {@link Thread} from a certain {@link Server} that communicates with
 * any of the other server instances to send requests.
 */
public class RequestSenderThread extends Thread{
    protected Socket socket;
    protected int request;
    protected Object objectToSend;
    protected String clientHost;
    protected int clientPort;
    protected int result;


    /**
     * Builds a request sender that will communicate with a specific {@link Server} determined by
     * the given {@link Socket}.
     * @param socket Socket of the communication with the server.
     * @param request {@link Util} code representing operation to perform.
     * @param objectToSend Information to send (it might be either a {@code String} or a {@link File}).
     */
    public RequestSenderThread(Socket socket, int request, Object objectToSend) {
        this.socket = socket;
        this.request = request;
        this.objectToSend = objectToSend;

        this.result = Util.NOT_READY;
    }


    /**
     * Builds a request sender that will communicate with a specific {@link Server} determined by
     * the given {@link Socket}. USE ONLY IF {@code request == Util.GET_FILE}.
     * @param socket Socket of the communication with the server.
     * @param request {@link Util} code representing operation to perform.
     * @param objectToSend Information to send (it might be either a {@code String} or a {@link File}).
     * @param clientHost Host of the client that would receive the information.
     * @param clientPort Port used to communicate with the client.
     */
    public RequestSenderThread(Socket socket, int request, Object objectToSend, String clientHost, int clientPort) {
        this.socket = socket;
        this.request = request;
        this.objectToSend = objectToSend;
        this.clientHost = clientHost;
        this.clientPort = clientPort;

        this.result = Util.NOT_READY;
    }


    @Override
    public void run() {
        try {
            // Crear los streams para el primer servidor parcial
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            // Mandarle al servidor seleccionado la petición del servidor general
            oos.writeInt(request);
            oos.flush();
            oos.writeObject(objectToSend);
            oos.flush();

            // Si se trata de una petición de obtención, ha de mandar información
            // adicional: la dirección del cliente que ha realizado la petición y
            // su puerto
            if (request == GET_FILE) {
                oos.writeObject(clientHost);
                oos.flush();
                oos.writeInt(clientPort);
                oos.flush();
            }

            int processCode = ois.readInt();
            switch (processCode) {
                case Util.FILE_DELETED:
                case Util.FILE_RETRIEVED:
                case Util.FILE_STORED:
                    result = Util.SUCCESS;
                    break;
                default:
                    result = Util.CRITICAL_ERROR;
            }
        }
        catch (IOException e) {
            result = Util.CRITICAL_ERROR;
        }
    }


    /**
     * Gets the current result of the ongoing process.
     * @return {@link Util} for success, critical error or not ready process.
     */
    public int getResult() {
        return result;
    }
}
