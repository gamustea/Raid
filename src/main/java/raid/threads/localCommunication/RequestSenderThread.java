package raid.threads.localCommunication;

import raid.misc.Util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static raid.misc.Util.*;


public class RequestSenderThread extends Thread{
    protected Socket socket;
    protected int request;
    protected Object objectToSend;
    protected String clientHost;
    protected int clientPort;

    protected int result;

    public RequestSenderThread(Socket socket, int request, Object objectToSend) {
        this.socket = socket;
        this.request = request;
        this.objectToSend = objectToSend;

        this.result = Util.NOT_READY;
    }

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

    public int getResult() {
        return result;
    }
}
