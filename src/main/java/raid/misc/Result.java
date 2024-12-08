package raid.misc;

/**
 * Result is a returning-based object that allows a method to
 * return two different types of data. Usually, it is used to
 * retrieve different instances of objects - however, it can be
 * used aswell to return data of the same class. Both parameters
 * of the object are accessible, but not settable, as their
 * purpose is no storing data but transporting it.
 *
 * @param <E> First class
 * @param <T> Second class
 */
public record Result<E, T>(E result1, T result2) {}
