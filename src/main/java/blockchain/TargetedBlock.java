package blockchain;

public class TargetedBlock extends Block {
    private final String target;

    public TargetedBlock(String author, String target) {
        super(author);
        this.target = target;
    }

    public String getTarget() {
        return target;
    }
}
