package raid.servers.files.strategies;

import java.io.File;
import java.net.Socket;

public class PartialSavingStrategy extends Strategy {
    public PartialSavingStrategy(String path, StrategyType strategyType) {
        super(path, strategyType);
    }

    @Override
    public String saveFile(File file) {
        return "";
    }

    @Override
    public String deleteFile(String file) {
        return "";
    }

    @Override
    public String getFile(String file, Socket clientSocket) {
        return null;
    }
}
