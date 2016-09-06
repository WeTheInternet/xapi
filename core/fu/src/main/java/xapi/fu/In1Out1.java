package xapi.fu;

import java.util.function.Function;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
@SuppressWarnings("unchecked") // yes, this api will let you do terrible things.  Don't do terrible things.
public interface In1Out1<I, O> extends Rethrowable, Lambda {

  In1Out1 IDENTITY = i->i;

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

  public static void main(String ... a) {
    In1Out1<String, String> i = In1Out1.IDENTITY;
    final String v = i.select(1, System.out::println, "", "thing");
    System.out.println(v);
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
    return in-> {
      final O o = this.io(in);
      return mapper.io(o);
    };
  }

  default <To> In1Out1<To,O> mapIn(In1Out1<To, I> mapper) {
    return to-> {
      final I i1 = mapper.io(to);
      return this.io(i1);
    };
  }

  static <I, T, O> In1Out1<I, O> ofMapped(In1Out1<I, T> mapper, In1Out1<T, O> job) {
    return mapper.mapOut(job);
  }


}
