package raid.servers.files;

import raid.servers.files.strategies.Strategy;

import java.io.File;
import java.net.Socket;

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

    public int getFile(String name, Socket clientSocket) {
        return strategy.getFile(name);
    }

    public void setStrategy(Strategy strategy) {
        this.strategy = strategy;
    }
}
