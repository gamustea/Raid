package executables;

import raid.clients.Client;
import raid.servers.CentralServer;

import java.io.File;

public class ClientMain1 {
    public static void main(String[] args) {
        new Client("localhost", 55554).boot();
    }
}
