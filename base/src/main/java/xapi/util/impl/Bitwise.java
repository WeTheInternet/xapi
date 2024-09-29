package xapi.util.impl;

/**
 * Need 32 boolean fields?
 * At one int-per-pointer for boolean fields, that's 128 bytes of heap.
 * Using one int value and one pointer to a Bitwise instance saves heap space,
 * at the price of some bitwise math and synchronization code.
 *
 * All bitwise add and remove operations are synchronized (and volatile).
 * All set and get operations are only volatile.
 *
 * This class may incur slight overhead in single-threaded implementations,
 * (provided the jvm doesn't delete locking code because it knows it's unithread)
 * but if used to represent multiple volatile synchronized boolean fields,
 * it can be a very effective means to share state between multiple threads.
 *
 * Note that this class will enforce a happens-before relationship between
 * threads writing to or reading from this value.
 *
 * If you have multiple sets of bools which should not enforce synchronicity
 * between each other, you should use separate Bitwise objects for each.
 * (This will also reduce the size of your bytecode constant pool,
 * by encouraging the reuse of 1,2,4,8,16, instead of 0x1->0xFFFFF)
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public final class Bitwise {

  /**
   * In order to allow unsynchronized reads, we have to use a volatile value.
   */
  private volatile int value;

  public Bitwise(int initialValue) {
    this.value = initialValue;
  }

  public boolean isTrue(int pos) {
    // because value is volatile, we only want to perform one read per operation
    return (value & pos) == pos;
  }

  public boolean isFalse(int pos) {
    // non-synchronized reads, to favor get() over set().
    return (value & pos) != pos;
  }
  /**
   * Applies the int value using bitwise |
   *
   * This method is synchronized to allow deterministic, concurrent access.
   *
   * For known single-threaded operations, (getValue() | bitwise) is faster.
   *
   * @param bitwise - Binary flags to set to true.
   * @return this, for chaining
   */
  public synchronized Bitwise setTrue(int bitwise) {
    // just because value is volatile does not mean instructions won't interleave
    value |= bitwise;
    return this;
  }

  /**
   * Applies the int value using bitwise &~ (NOR)
   *
   * This method is synchronized to allow deterministic, concurrent access.
   *
   * For known single-threaded operations, (getValue() &~ bitwise) is faster.
   *
   * @param bitwise - Binary flags to set to true.
   * @return this, for chaining
   */
  public synchronized Bitwise setFalse(int bitwise) {
    // value is volatile, but we still don't want instructions to interleave.
    value = value & ~bitwise;
    return this;
  }

  /**
   * @return The value stored in this register
   */
  public int getValue() {
    return value;
  }

  /**
   * This is the fastest way to set a value, as it is not synchronized.
   *
   * If you already know the or'd | together value,
   * prefer .setValue() as it only performs one volatile write,
   * instead of a synchronized, volatile read and write.
   *
   * If you are interleaving an overwrite and bitwise or operations
   * concurrently, you deserve undeterministic behavior;
   * synchro won't save you! :)
   *
   * @param value - The new value to use.
   */
  public void setValue(int value) {
    this.value = value;
  }
}
