package java.util.concurrent.locks;

/**
 * No-op implementation provided only so compile time references do not fail
 */
public class LockSupport {
    private LockSupport() {} // Cannot be instantiated.

    public static void unpark(Thread thread) {
    }

    public static void park(Object blocker) {
    }

    public static void parkNanos(Object blocker, long nanos) {
    }

    public static void parkUntil(Object blocker, long deadline) {
    }

    public static Object getBlocker(Thread t) {
        return t;
    }

    public static void park() {

    }

    public static void parkNanos(long nanos) {
    }

    public static void parkUntil(long deadline) {

    }

}
