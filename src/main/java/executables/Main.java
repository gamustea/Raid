package executables;

import raid.clients.Client;
import raid.servers.CentralServer;
import raid.servers.EastServer;
import raid.servers.Server;
import raid.servers.WestServer;
import raid.servers.threads.ClientManager;

public class Main {
    public static void main(String[] args) {
        ServerExecution se1 = new ServerExecution(new CentralServer());
        ServerExecution se2 = new ServerExecution(new EastServer());
        ServerExecution se3 = new ServerExecution(new WestServer());

        se1.start();
        se2.start();
        se3.start();

        new Client("localhost", Server.CENTRAL_CLIENT_PORT).boot();
    }
}
