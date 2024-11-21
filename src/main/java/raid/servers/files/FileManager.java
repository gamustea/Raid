package raid.servers.files;

import raid.servers.files.strategies.Strategy;

import java.io.File;

public class FileManager{
    private Strategy strategy;

    public FileManager(Strategy strategy) {
        this.strategy = strategy;
    }

    public int saveFile(File file) {
        return strategy.saveFile(file);
    }

    public int deleteFile(String file) {
        return strategy.deleteFile(file);
    }

    public int getFile(String name, String clientHost) {
        return strategy.getFile(name, clientHost);
    }

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }
}
