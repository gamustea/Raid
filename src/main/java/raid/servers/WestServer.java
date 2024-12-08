package raid.servers;

import raid.servers.files.PartialSavingStrategy;

import static raid.misc.Util.*;
import static raid.servers.files.StrategyType.West;

/**
 * Instance of {@link Server}. Follows a {@link PartialSavingStrategy},
 * and it's meant to treat files as a partial block, so that it stores half of the
 * file. It'll communicate
 * with the peripheral {@code Servers} (West and Central Servers).
 */
public class WestServer extends Server {
    public WestServer() {
        super();
        host = getProperty("WEST_HOST", PORTS);
        testPort = Integer.parseInt(getProperty("WEST_TEST_PORT", PORTS));
        port = Integer.parseInt(getProperty("WEST_CLIENT_PORT", PORTS));
        localCommunicationPort = Integer.parseInt(getProperty("WEST_LOCAL_CONNECTION_PORT", PORTS));
        strategy = new PartialSavingStrategy(SERVER_FILE_PATH + "\\RaidWest", West);
    }
}
