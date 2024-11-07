package raid.servers;

import raid.servers.files.strategies.PartialSavingStrategy;

import static raid.servers.files.strategies.StrategyType.East;

public class EastServer extends Server {
    public EastServer() {
        testPort = 55552;
        port = 55555;
        host = "localhost";
        localCommunicationPort = 55558;
        this.strategy = new PartialSavingStrategy("C:\\Users\\gmiga\\Documents\\RaidTesting\\RaidEast", East);
    }
}
