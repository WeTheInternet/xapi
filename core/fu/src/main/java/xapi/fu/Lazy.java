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
public class Lazy <T> implements Out1<T> {

  // This is volatile because the first read to this proxy will immutabilize the result of the constructot Out1,
  // thus, we want all other threads to notice that the Lazy has changed,
  // without calling the expensive synchronized proxy needed to safely "lock once" on a deferred value
  private volatile Out1<T> proxy;

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
        if (proxy == prox[0]) {
          // first on in tries the provider;
          T value = supplier.out1();
          // default acceptable value is non-null
          if (valueAcceptable(value)) {
            // We only override the proxy if the value is non-null,
            // or if you specifically implemented LazyNullable.
            proxy = immutable1(value);
            prox[0] = null;
          }
          return value;
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

  public final boolean isUnresolved() {
    return proxy instanceof ProxySupplier;
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

  public static <I, T> Lazy<T> mapped1(In1Out1<I, T> supplier, I value) {
    final Out1<T> factory = ()->supplier.io(value);
    return new Lazy<>(factory);
  }

  public static <I, T> Lazy<T> mapped1Unsafe(In1Out1Unsafe<I, T> supplier, I value) {
    final Out1<T> factory = ()->supplier.io(value);
    return new Lazy<>(factory);
  }

  public static <I, T> Lazy<T> deferred1(In1Out1<I, T> supplier, Out1<I> value) {
    final Out1<T> factory = ()->supplier.io(value.out1());
    return new Lazy<>(factory);
  }

  public static <I, T> Lazy<T> deferred1Unsafe(In1Out1Unsafe<I, T> supplier, Out1Unsafe<I> value) {
    final Out1<T> factory = ()->supplier.io(value.out1());
    return new Lazy<>(factory);
  }

  public static <I, T> Lazy<T> deferred1immediate2(In1Out1<I, T> supplier, Out1<I> value) {
    final I whenCreated = value.out1();
    final Out1<T> factory = ()->supplier.io(whenCreated);
    return new Lazy<>(factory);
  }

  public static <I, T> Lazy<T> deferred1immediate2Unsafe(In1Out1Unsafe<I, T> supplier, Out1Unsafe<I> value) {
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
