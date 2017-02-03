package xapi.fu;

import xapi.fu.In1Out1.In1Out1Unsafe;

/**
 * A lazy-initialized object.
 *
 * Beware that this lazy object will be synchronized upon every get until it return non-null,
 * unless you supply a concrete value to the constructor of the class.
 *
 * If you are supplying a value directly to a Lazy, be aware that Lazy.lazy1() and the default constructors do not tolerate nulls.
 * They will call assert valueAcceptable(ctorValue), which defaults to only allow non-null values.
 *
 * If you want to explicitly allow null, use {@link LazyNullable} or {@link Lazy#ofNullable(Object)}
 *
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 14/12/15.
 */
public class Lazy <T> implements Out1<T>, IsLazy {

  // This is volatile because the first read to this proxy will immutabilize the result of the constructot Out1,
  // thus, we want all other threads to notice that the Lazy has changed,
  // without calling the expensive synchronized proxy needed to safely "lock once" on a deferred value
  private volatile Out1<T> proxy;
  private volatile boolean resolving;

  @Override
  public final T out1() {
    return proxy.out1();
  }

  // First, the easy constructor... a reference to a value...
  public Lazy(T value) {
    // If you have a concrete value, skip the expensive proxy sync; either use an immutable wrapper, or the shared NULL pointer
    proxy = valueAcceptable(value) ? Immutable.immutable1(value) : NULL;
  }

  private interface ProxySupplier <T> extends Out1<T> { }

  public Immutable<T> asImmutable(){

    // If we are already immutable, share proxy
    if (proxy instanceof Immutable) {
      return (Immutable<T>) proxy;
    }

    // Get our value; if it is acceptable, our proxy will be immutable
    final T value = out1();

    // Try to share proxy again
    if (proxy instanceof Immutable) {
      return (Immutable<T>) proxy;
    }

    // Value was not acceptable, so return a new immutable instance.
    return Immutable.immutable1(value);
  }

  @SuppressWarnings("unchecked")
  public Lazy(Out1<T> supplier) {
    proxy = proxySupplier(supplier);
  }

  private Out1<T> proxySupplier(Out1<T> supplier) {
    // The constructor of Lazy closes over this constructor using this array as a writable reference.
    // Note that we are not putting this in a field because we don't want anyone to muck with it;
    final ProxySupplier<T>[] prox = new ProxySupplier[1];
    prox[0] = () -> {
      // take turns looking at the memory slot
      synchronized (prox) {
        resolving = true;
        try {
          if (proxy == prox[0]) {
            // first on in tries the provider;
            T value = supplier.out1();
            // default acceptable value is non-null
            if (valueAcceptable(value)) {
              // We only override the proxy if the value is non-null,
              // or if you specifically implemented LazyNullable.
              proxy = Immutable.immutable1(value);
              prox[0] = null;
            }
            return value;
          }
        } finally {
          resolving = false;
        }
      }
      return proxy.out1();
    };
    return prox[0];
  }

  /*
  The Default acceptable value is non-null.

  This exists for you to override.
  */
  protected boolean valueAcceptable(T value) {
    return value != null;
  }

  public static <T> Lazy<T> immutable1(T value) {
    return new Lazy<>(value == null ? NULL : new Lazy<>(value));
  }

  public final boolean isNull1() {
    // proxy is volatile, only pay to read it once.
    final Out1<T> value = proxy;
    // be as lazy as possible, but no lazier; safety first!
    return value == NULL || value == null || value.out1() == null;
  }

  public final boolean isFull1() {
    // proxy is volatile, only pay to read it once.
    final Out1<T> value = proxy;

    // be as lazy as possible; don't even call the method if we know it might be null.
    return value != NULL && value != null && value.out1() != null;
  }

  public final boolean isResolved() {
    return !isUnresolved();
  }

  /**
   * In case you want to know if someone else has already started resolving this Lazy,
   * this boolean is ONLY true when some other thread has started resolving us.
   *
   * This is useful in case you don't want to create the object,
   * but if someone else has started, then you should block (get the value),
   * so you can then clean it up (or if you only want it if it will already be paid for).
   *
   */
  public final boolean isResolving() {
    assert !resolving || isUnresolved(); // must not thing we are resolving when we are resolved.
    return resolving;
  }

  public final boolean isUnresolved() {
    return proxy instanceof ProxySupplier;
  }

  public final Lazy<T> ifFull(In1<T> callback) {
    return ifFull(callback, false);
  }
  public final Lazy<T> ifFull(In1<T> callback, boolean eager) {
    if (eager ? valueAcceptable(out1()) : isFull1()) {
      callback.in(out1());
    }
    return this;
  }

  public final Lazy<T> ifResolved(In1<T> callback) {
    if (isResolved()) {
      callback.in(out1());
    }
    return this;
  }

  public final Lazy<T> ifUnresolved(In1<T> callback) {
    if (isUnresolved()) {
      callback.in(out1());
    }
    return this;
  }

  public final Lazy<T> ifNull(In1<T> callback) {
    return ifNull(callback, false);
  }
  public final Lazy<T> ifNull(In1<T> callback, boolean eager) {
    if ((eager || isResolved()) // avoid resolving
        && isNull1()) {
      callback.in(out1());
    }
    return this;
  }

  public final T assertAcceptable1() {
    T out = out1();
    assert valueAcceptable(out);
    return out;
  }

  public final T assertNull1() {
    T out = out1();
    assert out == null && isNull1() && !isFull1();
    return out;
  }

  public final T assertFull1() {
    T out = out1();
    assert out != null : "Lazy " + this + ", returned value was null; expected it to be full; ";
    assert !isNull1() : "Lazy " + this + " .isNull1() returned true, despite out1() returning non-null";
    assert isFull1() : "Lazy " + this + " .isFull1() did not return true, despite out1() returning non-null";
    return out;
  }

  public static <T> Lazy<T> deferred1(Out1<T> supplier) {
    return supplier instanceof Lazy ?
        (Lazy<T>)supplier :
        new Lazy<>(supplier) ;
  }

  public static <I, T> Lazy<T> deferred1(In1Out1<I, T> supplier, I input) {
    return deferred1(supplier.supply(input));
  }

  public static <T> Lazy<T> deferred1Unsafe(Out1Unsafe<T> supplier) {
    return supplier instanceof Lazy ?
        (Lazy<T>)supplier :
        new Lazy<>(supplier) ;
  }

  public static <T> Lazy<T> immediate1(Out1<T> supplier) {
    return immutable1(supplier.out1());
  }

  public static <I, T> Lazy<T> immediate1(In1Out1<I, T> supplier, Out1<I> value) {
    return immutable1(supplier.apply(value));
  }

  public static <I, T> Lazy<T> immediate1(In1Out1<I, T> supplier, I value) {
    return immutable1(supplier.io(value));
  }

  public static <I, T> Lazy<T> immediate1Unsafe(In1Out1Unsafe<I, T> supplier, Out1<I> value) {
    return immutable1(supplier.apply(value));
  }

  public static <I, T> Lazy<T> immediate1Unsafe(In1Out1Unsafe<I, T> supplier, I value) {
    return immutable1(supplier.io(value));
  }

  public static <I, T> Lazy<T> deferSupplier(In1Out1<I, T> supplier, I value) {
    final Out1<T> factory = ()->supplier.io(value);
    return new Lazy<>(factory);
  }

  public static <I, T> Lazy<T> deferSupplierUnsafe(In1Out1Unsafe<I, T> supplier, I value) {
    final Out1<T> factory = ()->supplier.io(value);
    return new Lazy<>(factory);
  }

  public static <I, T> Lazy<T> deferBoth(In1Out1<I, T> supplier, Out1<I> value) {
    final Out1<T> factory = ()->supplier.io(value.out1());
    return new Lazy<>(factory);
  }

  public static <I1, I2, T> Lazy<T> deferAll(In2Out1<I1, I2, T> supplier, Out1<I1> o1, Out1<I2> o2) {
    final Out1<T> factory = ()->supplier.io(o1.out1(), o2.out1());
    return new Lazy<>(factory);
  }

  public static <I1, I2, I3, T> Lazy<T> deferAll(In3Out1<I1, I2, I3, T> supplier, Out1<I1> o1, Out1<I2> o2, Out1<I3> o3) {
    final Out1<T> factory = ()->supplier.io(o1.out1(), o2.out1(), o3.out1());
    return new Lazy<>(factory);
  }

  public static <I1, I2, T> Lazy<T> deferSupplier(In2Out1<I1, I2, T> supplier, I1 o1, I2 o2) {
    final Out1<T> factory = ()->supplier.io(o1, o2);
    return new Lazy<>(factory);
  }

  public static <I1, I2, I3, T> Lazy<T> deferSupplier(In3Out1<I1, I2, I3, T> supplier, I1 o1, I2 o2, I3 o3) {
    final Out1<T> factory = ()->supplier.io(o1, o2, o3);
    return new Lazy<>(factory);
  }

  public static <I, T> Lazy<T> deferSupplierUnsafe(In1Out1Unsafe<I, T> supplier, Out1Unsafe<I> value) {
    final Out1<T> factory = ()->supplier.io(value.out1());
    return new Lazy<>(factory);
  }

  public static <I, T> Lazy<T> deferSupplierImmediateValueUnsafe(In1Out1<I, T> supplier, Out1<I> value) {
    final I whenCreated = value.out1();
    final Out1<T> factory = ()->supplier.io(whenCreated);
    return new Lazy<>(factory);
  }

  public static <I, T> Lazy<T> deferSupplierImmediateValue(In1Out1Unsafe<I, T> supplier, Out1Unsafe<I> value) {
    final I whenCreated = value.out1();
    final Out1<T> factory = ()->supplier.io(whenCreated);
    return new Lazy<>(factory);
  }

  public static <T> LazyNullable<T> ofNullable(T value) {
    return new LazyNullable<>(value);
  }

  public static <T> LazyNullable<T> ofNullableDeferred(Out1<T> supplier) {
    return new LazyNullable<>(supplier);
  }

  public static <T> LazyNullable<T> ofNullableImmediate(Out1<T> supplier) {
    return ofNullable(supplier.out1());
  }

  public static <I, T> LazyNullable<T> ofNullable(In1Out1<I, T> supplier, I value) {
    final Out1<T> factory = ()->supplier.io(value);
    return new LazyNullable<>(factory);
  }

  public static <I, T> LazyNullable<T> ofNullableDeferred(In1Out1<I, T> supplier, Out1<I> value) {
    final Out1<T> factory = supplier.supplyDeferred(value);
    return new LazyNullable<>(factory);
  }

  public static <I, T> LazyNullable<T> ofNullableDeferredConcrete(In1Out1<I, T> supplier, I value) {
    final Out1<T> factory = supplier.supply(value);
    return new LazyNullable<>(factory);
  }

  public static <I, T> LazyNullable<T> ofNullableImmediate(In1Out1<I, T> supplier, Out1<I> value) {
    final Out1<T> factory = supplier.supplyImmediate(value);
    // Note that we do not resolve the supplier immediate, it only resolves the value immediately.
    // If you want to resolve the whole expression immediately, you should not be using a Lazy. :-)
    return new LazyNullable<>(factory);
  }

  public boolean isImmutable() {
    return proxy instanceof Immutable || proxy instanceof IsImmutable;
  }

  public final boolean isMutable() {
    return !isImmutable();
  }

  @SuppressWarnings({"all"})
  public static final Lazy LAZY_NULL = new LazyNullable(Out1.NULL);

  private static class LazyNullable<T> extends Lazy<T> {

    /**
     * If you don't want to take up RAM with a bunch of pointers to potentially null values,
     * consider using this memory address to represent a null provider (just beware you lose generic information!)
     */

    public LazyNullable(Out1<T> supplier) {
      super(supplier == null ? NULL : supplier);
    }

    public LazyNullable(T value) {
      super(value);
    }

    @Override
    protected boolean valueAcceptable(T value) {
      return true;
    }
  }

}
