package returning;

public class Result<E, T> {
    private E result1;
    private T result2;

    public Result(E result1, T result2) {
        this.result1 = result1;
        this.result2 = result2;
    }

    public E getResult1() {
        return result1;
    }

    public T getResult2() {
        return result2;
    }
}
