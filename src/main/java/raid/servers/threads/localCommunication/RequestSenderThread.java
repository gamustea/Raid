package raid.servers.threads.localCommunication;

import raid.misc.Util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import static raid.misc.Util.*;


public abstract class RequestSenderThread extends Thread{
    protected Socket socket;
    protected int request;
    protected int result;
    protected Object objectToSend;
    protected String clientPath;
    protected int port;

    protected RequestSenderThread(Socket socket, int request) {
        this.socket = socket;
        this.request = request;
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
            // adicional: la dirección del cliente que ha realizado la petición
            if (request == GET_FILE) {
                oos.writeObject(clientPath);
                oos.flush();
                oos.writeInt(port);
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
