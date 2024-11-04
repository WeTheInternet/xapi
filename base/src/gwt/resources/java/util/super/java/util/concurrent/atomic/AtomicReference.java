package java.util.concurrent.atomic;

import java.util.function.UnaryOperator;
import java.util.function.BinaryOperator;

public class AtomicReference<V> implements java.io.Serializable {

    private V value;

    public AtomicReference(V initialValue) {
        value = initialValue;
    }

    public AtomicReference() {
    }

    public final V get() {
        return value;
    }

    public final void set(V newValue) {
        value = newValue;
    }

    public final void lazySet(V newValue) {
        set(newValue);
    }

    public final boolean compareAndSet(V expect, V update) {
        if (value == expect) {
            value = update;
            return true;
        }
        return false;
    }

    public final boolean weakCompareAndSet(V expect, V update) {
        return compareAndSet(expect, value);
    }

    public final V getAndSet(V newValue) {
        V curValue = value;
        value = newValue;
        return curValue;
    }

    public final V getAndUpdate(UnaryOperator<V> updateFunction) {
        V curValue = get();
        value = updateFunction.apply(curValue);
        return curValue;
    }

    public final V updateAndGet(UnaryOperator<V> updateFunction) {
        return (value = updateFunction.apply(get()));
    }

    public final V getAndAccumulate(V x,
                                    BinaryOperator<V> accumulatorFunction) {
        V curValue = get();
        value = accumulatorFunction.apply(curValue, x);
        return curValue;
    }

    public final V accumulateAndGet(V x,
                                    BinaryOperator<V> accumulatorFunction) {
        V curValue = get();
        value = accumulatorFunction.apply(curValue, x);
        return value;
    }

    public String toString() {
        return String.valueOf(get());
    }

}
