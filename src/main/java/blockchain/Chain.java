package blockchain;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Queue;

public class Chain {
    private final Queue<Block> chain;
    private final File referedFile;

    public Chain(File file) {
        this.chain = new ArrayDeque<>();
        this.referedFile = file;
    }

    public Queue<Block> getChain() {
        return chain;
    }

    public File getReferedFile() {
        return referedFile;
    }
}
