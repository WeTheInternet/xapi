package java.util.concurrent.atomic;

import java.util.function.LongUnaryOperator;
import java.util.function.LongBinaryOperator;

public class AtomicLong extends Number implements java.io.Serializable {

  private long value;

  public AtomicLong(long initialValue) {
    value = initialValue;
  }

  public AtomicLong() {
  }

  public final long get() {
    return value;
  }

  public final void set(long newValue) {
    value = newValue;
  }

  public final void lazySet(long newValue) {
    set(newValue);
  }

  public final long getAndSet(long newValue) {
    long oldValue = value;
    value = newValue;
    return oldValue;
  }

  public final boolean compareAndSet(long expect, long update) {
    if (value == expect) {
      value = update;
      return true;
    }
    return false;
  }

  public final boolean weakCompareAndSet(long expect, long update) {
    return compareAndSet(expect, update);
  }

  public final long getAndIncrement() {
    return value++;
  }

  public final long getAndDecrement() {
    return value--;
  }

  public final long getAndAdd(long delta) {
    long oldValue = value;
    value += delta;
    return oldValue;
  }

  public final long incrementAndGet() {
    return ++value;
  }

  public final long decrementAndGet() {
    return --value;
  }

  public final long addAndGet(long delta) {
    return (value += delta);
  }

  public final long getAndUpdate(LongUnaryOperator updateFunction) {
    long oldValue = value;
    value = updateFunction.applyAsLong(oldValue);
    return oldValue;
  }

  public final long updateAndGet(LongUnaryOperator updateFunction) {
    return (value = updateFunction.applyAsLong(value));
  }

  public final long getAndAccumulate(long amount,
                                    LongBinaryOperator accumulatorFunction) {
    long oldValue = value;
    value = accumulatorFunction.applyAsLong(oldValue, amount);
    return oldValue;
  }

  public final long accumulateAndGet(long amount,
                                    LongBinaryOperator accumulatorFunction) {
    return (value = accumulatorFunction.applyAsLong(value, amount));
  }

  public String toString() {
    return value + "";
  }

  public int intValue() {
    return (int)value;
  }

  public long longValue() {
    return value;
  }

  public float floatValue() {
    return (float)value;
  }

  public double doubleValue() {
    return (double)value;
  }

}
