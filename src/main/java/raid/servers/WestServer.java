package raid.servers;

import raid.servers.files.strategies.PartialSavingStrategy;

import static raid.servers.files.strategies.StrategyType.West;

public class WestServer extends Server {
    public WestServer() {
        testPort = 55550;
        port = 55553;
        localCommunicationPort = 55556;
        host = "localhost";
        this.strategy = new PartialSavingStrategy("C:\\Users\\gmiga\\Documents\\RaidTesting\\RaidWest", West);
    }
}
