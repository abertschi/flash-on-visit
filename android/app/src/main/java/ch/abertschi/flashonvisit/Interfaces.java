package ch.abertschi.flashonvisit;

/**
 * Created by abertschi on 15.02.17.
 */
public class Interfaces {

    private Interfaces() {
    }

    public interface Consumer<T> {
        void accept(T t);
    }

    public interface Supplier<T> {
        T get();
    }

    public interface Function<T, R> {
        R apply(T t);
    }

    public interface Predicate<T> {
        boolean test(T t);
    }
}
