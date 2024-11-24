package raid.servers;

import raid.servers.files.PartialSavingStrategy;

import static raid.servers.files.StrategyType.East;

/**
 * Instance of {@link Server}. Follows a {@link PartialSavingStrategy},
 * and it's meant to treat files as a partial block, so that it stores half of the
 * file. It'll communicate
 * with the peripheral {@code Servers} (West and Central Servers).
 */
public class EastServer extends Server {
    public EastServer() {
        testPort = 55552;
        port = 55555;
        host = "localhost";
        localCommunicationPort = 55558;
        this.strategy = new PartialSavingStrategy("C:\\Users\\gmiga\\Documents\\RaidTesting\\RaidEast", East);
    }
}
