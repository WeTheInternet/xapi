import java.util.concurrent.TimeUnit;

/**
 * Minimal emulation for GWT; all our locks do nothing.
 */
public interface Lock {

    default void lock() {}

    default void lockInterruptibly() throws InterruptedException {}

    default boolean tryLock() { return true; }

    default boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return true;
    }

    default void unlock() {}
}
