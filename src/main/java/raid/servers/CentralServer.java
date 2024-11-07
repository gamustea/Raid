package raid.servers;

import raid.servers.files.strategies.FullSavingStrategy;

/**
 * Instance of {@link Server}. Follows a {@link FullSavingStrategy},
 * and it's meant to treat files as a full block. It'll communicate
 * with the peripheral {@code Servers} (West and East Servers).
 */
public class CentralServer extends Server {
    public CentralServer() {
        host = "localhost";
        testPort = 55551;
        port = 55554;
        localCommunicationPort = 55557;
        this.strategy = new FullSavingStrategy("C:\\Users\\gmiga\\Documents\\RaidTesting\\RaidCentral");
    }
}
