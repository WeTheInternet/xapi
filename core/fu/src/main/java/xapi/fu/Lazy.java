package xapi.fu;

import xapi.fu.In1Out1.In1Out1Unsafe;
import xapi.fu.has.HasResolution;

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
public class Lazy <T> implements Out1<T>, IsLazy, HasResolution {

  // This is volatile because the first read to this proxy will immutabilize the result of the constructot Out1,
  // thus, we want all other threads to notice that the Lazy has changed,
  // without calling the expensive synchronized proxy needed to safely "lock once" on a deferred value
  protected volatile Out1<T> proxy;
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
    proxy = simpleProxy(supplier);
  }

  /**
   * Creates a lazily-initialized variable that is immutable after it is resolved.
   *
   * This constructor allows you to specify a spy function that will be called
   * when the variable is successfully resolved, as well as whether you want
   * to call that spy function while still holding the lock,
   * or whether to release the lock before notifying the spy.
   *
   * By default, we will release the lock first, since that can avoid any
   * potential reentrance issues.  So, beware that mutating shared state
   * in your spy function will NOT, BY DEFAULT, BE SYNCHRONIZED ACROSS THREADS.
   *
   * @param supplier The function providing the value of our variable.
   * @param spy The function to call when the supplier returns not-null
   *            (or whatever you override {@link #valueAcceptable(Object)} to return true on)
   * @param spyBeforeUnlock Whether to call the spy before exiting synchronized intializer. Default false.
   *                        If you need to mutate state shared across threads in a manner that does not
   *                        have it's own memory synchronization, you can send true here,
   *                        and take advantage of the internal locking of our init proxy.
   *                        When sending true here, beware that you CANNOT
   */
  public Lazy(Out1<T> supplier, In1<T> spy, boolean spyBeforeUnlock) {
    proxy = complexProxy(supplier, spy, spyBeforeUnlock);
  }

  protected Out1<T> complexProxy(Out1<T> supplier, In1<T> spy, boolean spyBeforeUnlock) {
    // When the spy is a no-op, we'll use a much simpler version of this code
    if (spy == In1.IGNORED) {
      return simpleProxy(supplier);
    }
    // The constructor of Lazy closes over this constructor using this array as a writable reference.
    // Note that we are not putting this in a field because we don't want anyone to muck with it;
    final ProxySupplier<T>[] prox = new ProxySupplier[1];
    prox[0] = () -> {
      // take turns looking at the memory slot
      T resolved = null;
      try {
        synchronized (prox) {
          startResolving();
          try {
            if (proxy == prox[0]) {
              // first one in tries the provider;
              T value = resolved = supplier.out1();
              // default acceptable value is non-null
              if (valueAcceptable(value)) {
                // We only override the proxy if the value is non-null,
                // or if you specifically implemented LazyNullable.
                proxy = Immutable.immutable1(value);
                prox[0] = null;
                // let the spy see it after we've resolved the lazy, but before we return.
                if (spyBeforeUnlock) {
                  // the spy wants to act as a constructor to initialize the object before others see it.
                  spy.in(resolved);
                }
              } else {
                resolved = null;
              }
              return value;
            }
          } finally {
            finishResolving();
          }
        } // release synchronization lock
      } finally {
        if (!spyBeforeUnlock && resolved != null) {
          // the spy just wants to see the object after it's resolved,
          // but does not want to hold the lock preventing others from doing work
          // (if you have very expensive spies, it is important to get this right).
          spy.in(resolved);
        }
      }
      return proxy.out1();
    };
    return prox[0];
  }

  protected Out1<T> simpleProxy(Out1<T> supplier) {
    if (supplier instanceof IsImmutable) {
      return supplier;
    }
    // The constructor of Lazy closes over this constructor using this array as a writable reference.
    // Note that we are not putting this in a field because we don't want anyone to muck with it;
    final ProxySupplier<T>[] prox = new ProxySupplier[1];
    prox[0] = () -> {
      // take turns looking at the memory slot
      synchronized (prox) {
        startResolving();
        try {
          if (proxy == prox[0]) {
            // first one in tries the provider;
            final T value = supplier.out1();
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
          finishResolving();
        }
      } // release mutex
      return proxy.out1();
    };
    return prox[0];
  }

  private void startResolving() {
    // clearer message with stronger exception during development
    assert !resolving : "You are trying to access a Lazy while it is being initialized";
    if (resolving) {
      // smaller messages for production (we compile this code into js, so no need for huge unused messages)
      throw new IllegalStateException("no reentry");
    }
    resolving = true;
  }

  private void finishResolving() {
    resolving = false;
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

  @Override
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
    assert !resolving || isUnresolved(); // must not think we are resolving when we are resolved.
    return resolving;
  }

  @Override
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

  public static <T> Lazy<T> withSpy(Out1<T> supplier, In1<T> spy) {
    return new Lazy<>(supplier, spy, false);
  }

  public static <T> Lazy<T> withSpy(Out1<T> supplier, In1<T> spy, boolean spyBeforeUnlock) {
    return new Lazy<>(supplier, spy, spyBeforeUnlock);
  }

  public static <I, T> Lazy<T> deferred1(In1Out1<I, T> supplier, I input) {
    return deferred1(supplier.supply(input));
  }
  public static <I1, I2, T> Lazy<T> deferred1(In2Out1<I1, I2, T> supplier, I1 i1, I2 i2) {
    return deferred1(supplier.supply1(i1).supply(i2));
  }

  public static <T> Lazy<T> deferred1Unsafe(Out1Unsafe<T> supplier) {
    return supplier instanceof Lazy ?
        (Lazy<T>)supplier :
        new Lazy<>(supplier) ;
  }
  public static <T, I1> Lazy<T> deferred1Unsafe(In1Out1Unsafe<I1, T> supplier, I1 value) {
    return deferred1(supplier.supply(value));
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

  @Override
  public String toString() {
    if (isResolved()) {
      return "Lazy(resolved: " + out1() + ")";
    }
    return "Lazy(unresolved)";
  }
}
