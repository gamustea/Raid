package executables;

import raid.servers.Server;

public class ServerExecution extends Thread {
    private Server server;

    public ServerExecution(Server server) {
        this.server = server;
    }

    @Override
    public void run() {
        server.boot();
    }
}
