package xapi.fu;

/**
 * A lazy-initialized object.
 *
 * Beware that this lazy object will be synchronized upon every get.
 *
 * If you need a high-performance solution which uses volatile to reduce usage of synchronized,
 * @see xapi.inject.impl.SingletonProvider in xapi-core-inject package.
 *
 * That implementation is slightly more complex, but stops using synchronized after the value is initialized.
 *
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 14/12/15.
 */
public class Lazy <T> implements Out1<T> {

  public static <T> Out1<T> of(Out1<T> supplier) {
    return new Lazy<>(supplier);
  }

  public static <T> Out1<T> of(T value) {
    return new Lazy<>(value);
  }


  private Out1<T> proxy;

  public Lazy(Out1<T> supplier) {
    proxy = () -> {

      T value = supplier.out1();
      proxy = Out1.immutable1(value);
      return value;
    };
  }

  public Lazy(T value) {
    proxy = Out1.immutable1(value);
  }

  @Override
  public synchronized T out1() {
    return proxy.out1();
  }
}
