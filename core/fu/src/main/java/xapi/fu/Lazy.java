package xapi.fu;

/**
 * A lazy-initialized object.
 *
 * Beware that this lazy object will be synchronized upon every get until it return non-null.
 *
 * If you are supplying a value directly to a Lazy, be aware that Lazy.of() and the default constructors do not tolerate nulls.
 * They will call assert valueAcceptable(ctorValue), which defaults to only allow non-null values.
 *
 * If you want to explicitly allow null, use {@link LazyNullable} or {@link Lazy#ofNullable(Object)}
 *
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 14/12/15.
 */
public class Lazy <T> implements Out1<T> {

  public static class LazyNullable<T> extends Lazy<T> {

    public LazyNullable(Out1<T> supplier) {
      super(supplier);
    }

    public LazyNullable(T value) {
      super(value);
    }

    @Override
    protected boolean valueAcceptable(T value) {
      return true;
    }
  }

  public static <T> Lazy<T> of(T value) {
    return new Lazy<>(value);
  }

  public static <T> Lazy<T> ofDeferred(Out1<T> supplier) {
    return new Lazy<>(supplier);
  }

  public static <T> Lazy<T> ofImmediate(Out1<T> supplier) {
    return of(supplier.out1());
  }

  public static <I, T> Lazy<T> of(In1Out1<I, T> supplier, I value) {
    final Out1<T> factory = ()->supplier.io(value);
    return new Lazy<>(factory);
  }

  public static <I, T> Lazy<T> ofDeferred(In1Out1<I, T> supplier, Out1<I> value) {
    final Out1<T> factory = ()->supplier.io(value.out1());
    return new Lazy<>(factory);
  }

  public static <I, T> Lazy<T> ofDeferredConcrete(In1Out1<I, T> supplier, I value) {
    final Out1<T> factory = supplier.supply(value);
    return new Lazy<>(factory);
  }

  public static <I, T> Lazy<T> ofImmediate(In1Out1<I, T> supplier, Out1<I> value) {
    return of(supplier.apply(value));
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

  private volatile Out1<T> proxy;

  @SuppressWarnings("unchecked")
  public Lazy(Out1<T> supplier) {
    final Out1<T>[] prox = new Out1[1];
    prox[0] = () -> {
      synchronized (prox) {
        if (proxy == prox[0]) {
          T value = supplier.out1();
          if (valueAcceptable(value)) {
            // We only override the proxy if the value is non-null,
            // or if you specifically implemented LazyNullable
            proxy = Out1.immutable1(value);
            prox[0] = null;
          }
          return value;
        }
      }
      return proxy.out1();
    };
    proxy = prox[0];
  }

  protected boolean valueAcceptable(T value) {
    return value != null;
  }

  public Lazy(T value) {
    proxy = Out1.immutable1(value);
    assert valueAcceptable(value) : "You sent an unacceptable value ["+value+"]to lazy provider "+this+".";
  }

  @Override
  public synchronized T out1() {
    return proxy.out1();
  }
}
