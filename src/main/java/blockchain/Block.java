package blockchain;

import java.util.Date;

public class Block {
    private final Date date;
    private final String author;

    public Block(String author) {
        date = new Date(System.currentTimeMillis());
        this.author = author;
    }

    public Date getDate() {
        return date;
    }

    public String getAuthor() {
        return author;
    }
}
