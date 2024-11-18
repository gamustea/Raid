package raid;

public abstract class RS {
    public static final int GET_FILE = 1;
    public static final int SAVE_FILE = 2;
    public static final int DELETE_FILE = 3;
    public static final int CLOSE_CONNECTION = 4;

    public final static int NOT_READY = 10;
    public final static int SUCCESS = 11;
    public final static int FILE_NOT_FOUND = 12;
    public final static int CRITICAL_ERROR = 13;
}
