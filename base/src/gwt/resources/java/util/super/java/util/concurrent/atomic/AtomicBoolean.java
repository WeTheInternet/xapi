package java.util.concurrent.atomic;
public class AtomicBoolean implements java.io.Serializable {

    private boolean value;

    public AtomicBoolean(boolean initialValue) {
        value = initialValue;
    }

    public AtomicBoolean() {
    }

    public final boolean get() {
        return value;
    }

    public final boolean compareAndSet(boolean expect, boolean update) {
        if (value == expect) {
            value = update;
            return true;
        }
        return false;
    }

    public boolean weakCompareAndSet(boolean expect, boolean update) {
        return compareAndSet(expect, update);
    }

    public final void set(boolean newValue) {
        value = newValue;
    }

    public final void lazySet(boolean newValue) {
        set(newValue);
    }

    public final boolean getAndSet(boolean newValue) {
        boolean oldValue = value;
        value = newValue;
        return oldValue;
    }

    public String toString() {
        return "" + value;
    }

}
