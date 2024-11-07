package executables;

import raid.clients.Client;
import raid.servers.CentralServer;

public class ClientMain2 {
    public static void main(String[] args) {
        Client client = new Client("localhost", CentralServer.CENTRAL_CLIENT_PORT);
        client.boot();
    }
}
