package raid.threads.localCommunication;

import raid.servers.Server;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class NameRequestSenderThread extends RequestSenderThread {

    /**
     * Builds communication with an external {@link Server} to send them
     * a request related to the given request type. When started,
     * returns back a confirmation depending on the request.
     * @param socket {@link Socket} of the {@link Server} to contact
     * @param nameToSend Name of the {@link File} to manage
     * @param request Integer representing the request to perform; accesible
     *                from static instances of {@link Server}
     * @return Success message {@link String}, f.e.:
     * {@code "EXTERNAL SERVER REQUEST COMPLETED"}
     * @throws IOException if there has been an error building
     */
    public NameRequestSenderThread(Socket socket, String nameToSend, int request) {
        super(socket, request);
        this.objectToSend = nameToSend;
    }
}