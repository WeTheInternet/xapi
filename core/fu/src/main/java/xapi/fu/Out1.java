package xapi.fu;

import xapi.fu.In1Out1.In1Out1Unsafe;
import xapi.fu.Log.LogLevel;

import static xapi.fu.Filter.alwaysTrue;
import static xapi.fu.Immutable.immutable1;

import javax.inject.Provider;
import java.util.function.Supplier;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public interface Out1<O> extends Rethrowable {

  O out1();

  Out1 NULL = immutable1(null);
  static <T> Out1 <T> null1() {
      return NULL;
  }
  Out1<String> EMPTY_STRING = immutable1("");
  Out1<String> NEW_LINE = immutable1("\n");
  Out1<String> SPACE = immutable1(" ");
  Out1<Boolean> FALSE = immutable1(false);
  Out1<Integer> ZERO = immutable1(0);
  Out1<Boolean> TRUE = immutable1(true);
  Out1<Integer> ONE = immutable1(1);
  Out1<Integer> NEGATIVE_ONE = immutable1(-1);

  default boolean isImmutable() {
    Object o = this;
    return o instanceof Immutable || o instanceof IsImmutable;
  }

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

  static <O> Out1<O> out1Provider(Provider<O> of) {
    return of::get;
  }

  static <I, O> Out1<O> out1Immediate(In1Out1<I, O> mapper, I input) {
    return mapper.supply(input);
  }

  static <I, O> Out1<O> out1Immediate(In1Out1<I, O> mapper, Out1<I> input) {
    I value = input.out1();
    return ()->mapper.io(value);
  }

  static <I, O> Out1<O> out1Deferred(In1Out1<I, O> mapper, Out1<I> input) {
    return ()->mapper.io(input.out1());
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

    default O eatExceptions() {
      return eatExceptions(
          Log.firstLog(this), LogLevel.WARN, alwaysTrue());
    }

    @SuppressWarnings("unchecked")
    default O eatExceptions(Log log) {
      Filter<Throwable> filter = Filter.TRUE;
       return eatExceptions(log, LogLevel.WARN, filter);
    }

    default O eatExceptions(Log log, Filter<Throwable> filter) {
      return eatExceptions(log, LogLevel.WARN, filter);
    }

    @SuppressWarnings("unchecked")
    default O eatExceptions(Log log, LogLevel level) {
      return eatExceptions(log, level, alwaysTrue());
    }

    default O eatExceptions(Log log, LogLevel level, Filter<Throwable> filter) {
      try {
        return outUnsafe();
      } catch (Throwable e) {
        log = Log.normalize(log);
        if (log.isLoggable(level)) {
          try {
            log.log(getClass(), e);
          } catch (Throwable loggingError) {
            log.log(LogLevel.ERROR, "Error logging error! Errorception: ");
            log.log(LogLevel.ERROR, "Error while logging: ", loggingError);
            log.log(LogLevel.ERROR, "Original error: ", e);
          }
        }
        throw rethrow(e);
      }
    }
  }

  default <To> Out1<To> map(In1Out1<O, To> factory) {
    return factory.supplyDeferred(this);
  }

  default Out1<O> mapIf(In1Out1<O, Boolean> filter, In1Out1<O, O> modifier) {
    return ()->{
        O o = out1();
        if (filter.io(o)) {
            o = modifier.io(o);
        }
        return o;
    };
  }

  default <To> Out1<To> mapIf(In1Out1<O, Boolean> filter,
                              In1Out1<O, To> pass,
                              In1Out1<O, To> fail) {
    return ()->{
        O o = out1();
        if (filter.io(o)) {
            return pass.io(o);
        }
        return fail.io(o);
    };
  }

  default <In, To> Out1<To> map(In2Out1<O, In, To> factory, In in1) {
    return factory.supply(out1(), in1);
  }

  default <In, To> Out1<To> mapDeferred(In2Out1<O, In, To> factory, Out1<In> in1) {
    return factory.supply1Deferred(this).supplyDeferred(in1);
  }

  default <In, To> Out1<To> mapImmediate(In2Out1<O, In, To> factory, Out1<In> in1) {
    return factory.supply1(out1()).supplyDeferred(in1);
  }

  default <To> Out1<To> mapUnsafe(In1Out1Unsafe<O, To> factory) {
    return factory.supplyDeferred(this);
  }

}
