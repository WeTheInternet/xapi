package xapi.fu;

import java.util.function.Function;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
@SuppressWarnings("unchecked") // yes, this api will let you do terrible things.  Don't do terrible things.
public interface
In1Out1<I, O> extends Rethrowable, Lambda {

  In1Out1 IDENTITY = X_Fu::identity;
  In1Out1 DOWNCAST = X_Fu::downcast;
  In1Out1<Integer, Integer> INCREMENT_INT = i->i==null?1:i+1;
  In1Out1<Integer, Integer> DECREMENT_INT = i->i==null?-1:i-1;
  In1Out1 RETURN_NULL = X_Fu::returnNull;
  In1Out1 RETURN_TRUE = ignored->true;
  In1Out1 RETURN_FALSE = ignored->false;

  static <O> In1Out1<O, O> identity() {
    return IDENTITY;
  }

  static <F extends T, T> In1Out1<F, T> downcast() {
    return DOWNCAST;
  }

  static <I, O> In1Out1<I, O> returnNull() {
    return RETURN_NULL;
  }

  static <I, O> In1Out1<I, O> returnTrue() {
    return RETURN_TRUE;
  }

  static <I, O> In1Out1<I, O> returnFalse() {
    return RETURN_FALSE;
  }

  static <I, O> O[] mapArray(O[] into, In1Out1<I, O> mapper, I ... is) {
    if (into.length == 0) {
      into = X_Fu.copy(into, is.length);
    }
    if (is.length > into.length) {
      throw new IllegalArgumentException("Cannot map a larger number of inputs than outputs: " + into.length + " < " + is.length);
    }
    if (is.length < into.length) {
      if (into.length % is.length != 0) {
        throw new IllegalArgumentException("Cannot map an uneven multiple of inputs versus outputs: " + into.length + " % " + is.length + " != 0");
      }
      for (int i = 0; i < into.length; i++) {
        final O out = mapper.io(is[i%is.length]);
        into[i] = out;
      }
    } else {
      for (int i = 0; i < into.length; i++) {
        final O out = mapper.io(is[i]);
        into[i] = out;
      }
    }
    return into;
  }

  static <O, I extends O> In1Out1<I, O> identityNarrowed() {
    return IDENTITY;
  }

  O io(I in);

  default O apply(Out1<I> supplier) {
    return io(supplier.out1());
  }

  default I select(int position, In1<O> callback, I ... values) {
    final I in = values[position];
    final O out = io(in);
    callback.in(out);
    return in;
  }

  default Function<I, O> toFunction() {
    return this::io;
  }

  static <I, O> In1Out1<I, O> of(In1Out1<I, O> lambda) {
    return lambda;
  }

  static <I, O> In1Out1<I, O> ofDeferred(Out1<O> supplier) {
    return ignored->supplier.out1();
  }

  static <I, O> In1Out1<I, O> ofImmediate(Out1<O> supplier) {
    return of(supplier.out1());
  }

  static <I, O> In1Out1<I, O> of(O value) {
    return ignored->value;
  }

  static <I, O> In1Out1<I, O> of(In1<I> in, Out1<O> out) {
    return i-> {
      in.in(i);
      @SuppressWarnings("redundant") // let debuggers have a look. JIT can inline
      final O o = out.out1();
      return o;
    };
  }

  static <I1, I2, O> In1Out1<I2, O> from2_1(In2Out1<I1, I2, O> in, I1 out) {
    return in.supply1(out);
  }

  static <I1, I2, O> In1Out1<I1, O> from2_2(In2Out1<I1, I2, O> in, I2 out) {
    return in.supply2(out);
  }

  /**
   * This method just exists to give you somewhere to create a lambda that will rethrow exceptions.
   */
  static <I, O> In1Out1<I, O> unsafe(In1Out1Unsafe<I, O> of) {
    return of;
  }

  default Out1<O> supply(I in) {
    return () -> io(in);
  }

  default Out1<O> supplyDeferred(Out1<I> in) {
    return () -> io(in.out1());
  }

  default Out1<O> supplyImmediate(Out1<I> in) {
    return supply(in.out1());
  }

  default In1<I> adapt(In1<O> into) {
    return i->into.in(io(i));
  }

  default In1<I> ignoreOutput() {
    return this::io;
  }

  interface In1Out1Unsafe <I, O> extends In1Out1<I, O>, Rethrowable{
    O ioUnsafe(I in) throws Throwable;

    default O io(I in) {
      try {
        return this.ioUnsafe(in);
      } catch (Throwable e) {
        throw rethrow(e);
      }
    }
  }

  default <To> In1Out1<I,To> mapOut(In1Out1<O, To> mapper) {
    // technically we can use `this.` in the lambda, and it should work,
    // but that's not very readable as it requires moderately deep knowledge
    // of the JVM to know this just by reading code.
    final In1Out1<I, O> self = this;
    return in-> {
      final O o = self.io(in);
      return mapper.io(o);
    };
  }

  default In1Out1<I, O> mapIfNull(In1Out1<I, O> other) {
    if (other == RETURN_NULL) {
      // no need to indirect to a known-null-returner :-)
      return this;
    }
    final In1Out1<I, O> self = this;
    return i->{
      O mine = self.io(i);
      if (mine == null) {
        return other.io(i);
      }
      return mine;
    };
  }

  default In1Out1<I, O> firstNotNull(In1Out1<I, O> ... other) {
    final In1Out1<I, O> self = this;
    return i->{
      O val = self.io(i);
      if (val != null) {
        return val;
      }
      for (In1Out1<I, O> maybe : other) {
        val = maybe.io(i);
        if (val != null) {
          return val;
        }
      }
      return null;
    };
  }

  default <To> In1Out1<To,O> mapIn(In1Out1<To, I> mapper) {
    return to-> {
      final I i1 = mapper.io(to);
      return this.io(i1);
    };
  }

  default In1Out1Unsafe<I, O> unsafeIn1Out1() {
    return this instanceof In1Out1Unsafe ? (In1Out1Unsafe<I, O>) this : this::io;
  }

  static <I, T, O> In1Out1<I, O> ofMapped(In1Out1<I, T> mapper, In1Out1<T, O> job) {
    return mapper.mapOut(job);
  }

  static <I, O> O[] forEach(I[] is, O[] os, In1Out1<I, O> mapper) {
    for (int i = 0; i < os.length; i++) {
      if (i >= is.length) {
        return os;
      }
      O mapped = mapper.io(is[i]);
      os[i] = mapped;
    }
    return os;
  }

    default In1Out1<I, O> lazy(In2Out1<I, In1Out1<I, O>, O> cache) {
      return i-> cache.io(i, this);
    }

    default In1Out1<I, O> spyOut(In1<O> spy) {
      return i-> {
        final O o = io(i);
        spy.in(o);
        return o;
      };
    }

    static <I, O> In1Out1<I, O> alwaysThrow(Out1<Throwable> factory) {
      return unsafe(ignored->{
          throw factory.out1();
      });
    }

    static <I, O> In1Out1<I, O> alwaysThrowFrom(In1Out1<I, Throwable> factory) {
      return unsafe(value->{
          throw factory.io(value);
      });
    }
}
