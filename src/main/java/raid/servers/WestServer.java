package raid.servers;

import raid.servers.files.PartialSavingStrategy;

import static raid.servers.files.StrategyType.West;

/**
 * Instance of {@link Server}. Follows a {@link PartialSavingStrategy},
 * and it's meant to treat files as a partial block, so that it stores half of the
 * file. It'll communicate
 * with the peripheral {@code Servers} (West and Central Servers).
 */
public class WestServer extends Server {
    public WestServer() {
        testPort = 55550;
        port = 55553;
        localCommunicationPort = 55556;
        host = "localhost";
        this.strategy = new PartialSavingStrategy("C:\\Users\\gmiga\\Documents\\RaidTesting\\RaidWest", West);
    }
}
