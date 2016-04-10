package xapi.fu;

import xapi.fu.In1Out1.In1Out1Unsafe;

import javax.inject.Provider;
import java.util.function.Supplier;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public interface Out1<O> extends Rethrowable {

  O out1();

  default Out1<O> use(In1<O> callback) {
    callback.in(out1());
    return this;
  }

  /**
   * @return an immutable copy of this provider.
   */
  default <F extends Out1<O> & Frozen> F freeze() {
    if (this instanceof Frozen) {
      return (F) this;
    }
    final O o = out1();
    F f = (F)(Out1<O> & Frozen)()->o;
    return f;
  }

  default Out1<O> self() {
    return this;
  }
  default Supplier<O> toSupplier() {
    return this::out1;
  }
  default Provider<O> toProvider() {
    return this::out1;
  }

  static <O> Out1<O> out1Supplier(Supplier<O> of) {
    return of::get;
  }

  static <O> Out1<O> out1(Out1<O> of) {
    return of;
  }

  static <O> Out1Immutable<O> immutable1(O of) {
    return new Out1Immutable<>(of);
  }

  static <O> Out1<O> out1Provider(Provider<O> of) {
    return of::get;
  }

  /**
   * This method just exists to give you somewhere to create a lambda that will rethrow exceptions,
   * but exposes an exceptionless api.  If you don't have to call code with checked exceptions,
   * prefer the standard {@link #out1(Out1)}, as try/catch can disable / weaken some JIT compilers.
   */
  static <O> Out1<O> out1Unsafe(Out1Unsafe<O> of) {
    return of;
  }


  interface Out1Unsafe <O> extends Out1<O>, Rethrowable{
    O outUnsafe() throws Throwable;

    default O out1() {
      try {
        return outUnsafe();
      } catch (Throwable e) {
        throw rethrow(e);
      }
    }
  }

  class Out1Immutable <O> implements Out1<O> {

    private final O value;

    public Out1Immutable(O value) {
      this.value = value;
    }

    @Override
    public O out1() {
      return value;
    }

    public Out1Immutable <O> orElse(O maybeNull) {
      if (value == null) {
        return new Out1Immutable<>(maybeNull);
      }
      return this;
    }

    public Out1Immutable <O> orElse(Out1<O> provider) {
      if (value == null) {
        return new Out1Immutable<>(provider.out1());
      }
      return this;
    }
  }

  default <To> Out1<To> map(In1Out1<O, To> factory) {
    return factory.supplyDeferred(this);
  }

  default <To> Out1<To> mapUnsafe(In1Out1Unsafe<O, To> factory) {
    return factory.supplyDeferred(this);
  }
}
