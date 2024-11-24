package raid.servers;

import raid.servers.files.FullSavingStrategy;
import static raid.misc.Util.getProperty;

/**
 * Instance of {@link Server}. Follows a {@link FullSavingStrategy},
 * and it's meant to treat files as a full block. It'll communicate
 * with the peripheral {@code Servers} (West and East Servers).
 */
public class CentralServer extends Server {
    public CentralServer() {
        host = getProperty("CENTRAL_HOST", PORTS);
        testPort = Integer.parseInt(getProperty("CENTRAL_TEST_PORT", PORTS));
        port = Integer.parseInt(getProperty("CENTRAL_CLIENT_PORT", PORTS));
        localCommunicationPort = Integer.parseInt(getProperty("CENTRAL_LOCAL_CONNECTION_PORT", PORTS));
        this.strategy = new FullSavingStrategy("C:\\Users\\gmiga\\Documents\\RaidTesting\\RaidCentral");
    }
}
