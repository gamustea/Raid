package raid.threads.localCommunication;

import raid.RS;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


public abstract class RequestSenderThread extends Thread{
    protected Socket socket;
    protected int request;
    protected int result;
    protected Object objectToSend;

    protected RequestSenderThread(Socket socket, int request) {
        this.socket = socket;
        this.request = request;
        this.result = RS.NOT_READY;
    }

    @Override
    public void run() {
        try {
            // Crear los streams para el primer servidor parcial
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            // Pedirle al primer servidor que guarde la primera mitad
            // del fichero y recibir un mensaje
            oos.writeInt(request);
            oos.flush();
            oos.writeObject(objectToSend);
            oos.flush();

            String message = (String) ois.readObject();
            result = RS.SUCCESS;
        }
        catch (IOException | ClassNotFoundException e) {
            result = RS.CRITICAL_ERROR;
        }
    }

    public int getResult() {
        return result;
    }
}
