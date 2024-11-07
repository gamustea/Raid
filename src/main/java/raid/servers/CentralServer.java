package raid.servers;

import raid.servers.files.strategies.FullSavingStrategy;

public class CentralServer extends Server {
    public CentralServer() {
        host = "localhost";
        testPort = 55551;
        port = 55554;
        localCommunicationPort = 55557;
        this.strategy = new FullSavingStrategy("C:\\Users\\gmiga\\Documents\\RaidTesting\\RaidCentral");
    }
}
