package raid.servers.threads;

import raid.servers.Server;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class FileSenderThread extends Thread{
    private final Socket socket;
    private File fileToSend = null;
    private String fileToSearch = null;
    private final int request;
    private String result;

    /**
     * Builds communication with an external {@link Server} to send them
     * a request related to the given request type. When started,
     * returns back a confirmation depending on the request.
     * @param socket {@link Socket} of the {@link Server} to contact
     * @param fileToSend {@link File} to send
     * @param request Integer representing the request to perform; accesible
     *                from static instances of {@link Server}
     * @return Success message {@link String}, f.e.:
     * {@code "EXTERNAL SERVER REQUEST COMPLETED"}
     * @throws IOException if there has been an error building
     */
    public FileSenderThread(Socket socket, File fileToSend, int request) {
        this.socket = socket;
        this.fileToSend = fileToSend;
        this.request = request;
        this.result = "NOT FINISHED YET";
    }

    public FileSenderThread(Socket socket, String fileToSearch, int request) {
        this.socket = socket;
        this.fileToSearch = fileToSearch;
        this.request = request;
        this.result = "NOT FINISHED YET";
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
            if (request == Server.SAVE_FILE) {
                oos.writeObject(fileToSend);
            }
            else {
                oos.writeObject(fileToSearch);
            }
            oos.flush();

            result = (String) ois.readObject();
        }
        catch (IOException | ClassNotFoundException e) {
            result = e.getMessage();
        }
    }

    public String getResult() {
        return result;
    }
}
