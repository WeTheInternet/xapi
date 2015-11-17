package xapi.fu;

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

  default Supplier<O> toSupplier() {
    return this::out1;
  }
  default Provider<O> toProvider() {
    return this::out1;
  }

  static <O> Out1<O> out1(Supplier<O> of) {
    return of::get;
  }

  static <O> Out1<O> out1(O of) {
    return ()->of;
  }

  static <O> Out1<O> out1(Provider<O> of) {
    return of::get;
  }

  /**
   * This method just exists to give you somewhere to create a lambda that will rethrow exceptions,
   * but exposes an exceptionless api.  If you don't have to call code with checked exceptions,
   * prefer the standard {@link #out1(Supplier)}, as try/catch can disable / weaken some JIT compilers.
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
}
