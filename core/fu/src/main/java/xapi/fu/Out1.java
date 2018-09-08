package xapi.fu;

import xapi.fu.In1Out1.In1Out1Unsafe;
import xapi.fu.Log.LogLevel;
import xapi.fu.iterate.SingletonIterator;
import xapi.fu.iterate.SizedIterable;

import javax.inject.Provider;
import java.util.function.Supplier;

import static xapi.fu.Filter.alwaysTrue;
import static xapi.fu.Immutable.immutable1;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public interface Out1<O> extends Rethrowable, Lambda, HasMutability {

  O out1();

  Out1 NULL = immutable1(null);
  static <T> Out1 <T> null1() {
      return NULL;
  }

  Out1<String> EMPTY_STRING = immutable1("");
  Out1<String> FALSE_STRING = immutable1("false");
  Out1<String> TRUE_STRING = immutable1("true");
  Out1<String> SPACE = immutable1(" ");
  Out1<String> NEW_LINE = immutable1("\n");

  Out1<Boolean> FALSE = immutable1(false);
  Out1<Boolean> TRUE = immutable1(true);

  Out1<Integer> ZERO = immutable1(0);
  Out1<Integer> ONE = immutable1(1);
  Out1<Integer> NEG_ONE = immutable1(-1);

  Out1<Double> ONE_DOT = immutable1(1.);
  Out1<Double> NEG_ONE_DOT = immutable1(-1.);

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

  static <O> Out1<O> newOut1(Out1<O> of) {
    return of;
  }

  static <O> Out1<O> out1Provider(Provider<O> of) {
    return of::get;
  }

  static <I, O> Out1<O> out1Immediate(In1Out1<I, O> mapper, I input) {
    final O value = mapper.io(input);
    return immutable1(value);
  }


  static <I, O> Out1<O> out1DeferBoth(In1Out1<I, O> mapper, Out1<I> input) {
    return ()->mapper.io(input.out1());
  }

  static <I, O> Out1<O> out1Deferred(In1Out1<I, O> mapper, I input) {
    return ()->mapper.io(input);
  }

  /**
   * This method just exists to give you somewhere to create a lambda that will rethrow exceptions,
   * but exposes an exceptionless api.  If you don't have to call code with checked exceptions,
   * prefer the standard {@link #newOut1(Out1)}, as try/catch can disable / weaken some JIT compilers.
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
        Log.tryLog(Out1.class, this,"msg", e);
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

  default Out1<O> ifNull(In1Out1<O, O> mapper) {
    return mapIf(X_Fu::isNull, mapper);
  }

  default Out1<O> mapIf(In1Out1<O, Boolean> filter, In1Out1<O, O> mapper) {
    return ()->{
        O o = out1();
        if (filter.io(o)) {
            o = mapper.io(o);
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
    return factory
          .supply1Deferred(this)
          .supply(in1);
  }

  default <In1, In2, To> Out1<To> map(In3Out1<O, In1, In2, To> factory, In1 in1, In2 in2) {
    return factory
        .supply1Deferred(this)
        .supply1(in1)
        .supply(in2);
  }

  default <In, To> Out1<To> mapDeferred(In2Out1<O, In, To> factory, Out1<In> in1) {
    return factory.supply1Deferred(this).supplyDeferred(in1);
  }

  default <In1, To> In1Out1<In1, To> mapDeferred(In2Out1<O, In1, To> factory) {
    return factory.supply1Deferred(this);
  }

  default <In1, To> In1Out1<In1, To> mapImmediate(In2Out1<O, In1, To> factory) {
    return factory.supply1Immediate(this);
  }

  default <In, To> Out1<To> mapImmediate(In2Out1<O, In, To> factory, Out1<In> in1) {
    return factory.supply1(out1()).supplyDeferred(in1);
  }

  default <To> Out1<To> mapUnsafe(In1Out1Unsafe<O, To> factory) {
    return factory.supplyDeferred(this);
  }

  static <O> O[] resolve(O[] into, Out1<O> ... args) {
    if (args.length != into.length) {
      throw new IllegalArgumentException();
    }
    for (int i = 0; i < args.length; i++) {
      into[i] = args[i].out1();
    }
    return into;
  }

  static <O> O[] resolve(In1Out1<Integer, O[]> into, Out1<O> ... args) {
    final O[] result = into.io(args.length);
    for (int i = 0; i < args.length; i++) {
      result[i] = args[i].out1();
    }
    return result;
  }

  static <O, To> To[] resolveMapped(To[] into, In1Out1<O, To> mapper, Out1<O> ... args) {
    if (args.length != into.length) {
      throw new IllegalArgumentException();
    }
    for (int i = 0; i < args.length; i++) {
      final O val = args[i].out1();
      final To to = mapper.io(val);
      into[i] = to;
    }
    return into;
  }

  static <O, To> To[] resolveMapped(In1Out1<Integer, To[]> into, In1Out1<O, To> mapper, Out1<O> ... args) {
    final To[] result = into.io(args.length);
    for (int i = 0; i < args.length; i++) {
      final O val = args[i].out1();
      final To to = mapper.io(val);
      result[i] = to;
    }
    return result;
  }

    default Do ignoreOut1() {
      return this::out1;
    }

    default <I1> In1Out1<I1, O> ignoreIn1() {
      return i->out1();
    }

    default <I1, I2> In2Out1<I1, I2, O> ignoreIn2() {
      return (ig, nored)->out1();
    }

  default <O1> Out2<O1, O> return1(O1 obj) {
    Out1[] items = new Out1[] {
        immutable1(obj),
        this
    };
    return ()->items;
  }

  default <O1> Out2<O,O1> return2(O1 obj) {
    Out1[] items = new Out1[] {
        this,
        immutable1(obj)
    };
    return ()->items;
  }

  default <O1> Out2<O1, O> supply1(Out1<O1> obj) {
    return Out2.out2(obj, this);
  }

  default <O1> Out2<O,O1> supply2(Out1<O1> obj) {
    return Out2.out2(this, obj);
  }

  default Out1<O> spy1(In1<O> spy) {
    final Out1<O> self = this;
    return ()->{
      O val = self.out1();
      spy.in(val);
      return val;
    };
  }

    static <O1, O2> Out1[] toArray(O1 o1, O2 o2) {
      return new Out1[]{
        immutable1(o1), immutable1(o2)
      };
    }

  default SizedIterable<O> asIterable() {
    final SingletonIterator<Out1<O>> itr = SingletonIterator.singleItem(this);
    return itr.map(Out1::out1);
  }

  static <O> In2Out1<Out1<O>, Out1<O>, Out1<O>> reducer(In2Out1<O, O, O> valueReducer) {
    return (a, b) -> () -> valueReducer.io(a.out1(), b.out1());
  }

  default <F, M> Out1<M> merge(Out1<F> other, In2Out1<O, F, M> mapper) {
    return ()->mapper.io(out1(), other.out1());
  }
}
