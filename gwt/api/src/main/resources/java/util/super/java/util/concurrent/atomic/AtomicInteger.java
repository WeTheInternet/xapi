package java.util.concurrent.atomic;

import java.util.function.IntUnaryOperator;
import java.util.function.IntBinaryOperator;

public class AtomicInteger extends Number implements java.io.Serializable {

  private int value;

  public AtomicInteger(int initialValue) {
    value = initialValue;
  }

  public AtomicInteger() {
  }

  public final int get() {
    return value;
  }

  public final void set(int newValue) {
    value = newValue;
  }

  public final void lazySet(int newValue) {
    set(newValue);
  }

  public final int getAndSet(int newValue) {
    int oldValue = value;
    value = newValue;
    return oldValue;
  }

  public final boolean compareAndSet(int expect, int update) {
    if (value == expect) {
      value = update;
      return true;
    }
    return false;
  }

  public final boolean weakCompareAndSet(int expect, int update) {
    return compareAndSet(expect, update);
  }

  public final int getAndIncrement() {
    return value++;
  }

  public final int getAndDecrement() {
    return value--;
  }

  public final int getAndAdd(int delta) {
    int oldValue = value;
    value += delta;
    return oldValue;
  }

  public final int incrementAndGet() {
    return ++value;
  }

  public final int decrementAndGet() {
    return --value;
  }

  public final int addAndGet(int delta) {
    return (value += delta);
  }

  public final int getAndUpdate(IntUnaryOperator updateFunction) {
    int oldValue = value;
    value = updateFunction.applyAsInt(oldValue);
    return oldValue;
  }

  public final int updateAndGet(IntUnaryOperator updateFunction) {
    return (value = updateFunction.applyAsInt(value));
  }

  public final int getAndAccumulate(int amount,
                                    IntBinaryOperator accumulatorFunction) {
    int oldValue = value;
    value = accumulatorFunction.applyAsInt(oldValue, amount);
    return oldValue;
  }

  public final int accumulateAndGet(int amount,
                                    IntBinaryOperator accumulatorFunction) {
    return (value = accumulatorFunction.applyAsInt(value, amount));
  }

  public String toString() {
    return value + "";
  }

  public int intValue() {
    return value;
  }

  public long longValue() {
    return (long)value;
  }

  public float floatValue() {
    return (float)value;
  }

  public double doubleValue() {
    return (double)value;
  }

}
