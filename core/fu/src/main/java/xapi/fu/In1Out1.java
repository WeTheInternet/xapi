package xapi.fu;

import java.util.function.Function;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
@SuppressWarnings("unchecked") // yes, this api will let you do terrible things.  Don't do terrible things.
public interface In1Out1<I, O> extends Rethrowable {

  O io(I in);

  default int accept(int position, In1<O> callback, Object... values) {
    final I in = (I) values[position++];
    final O out = io(in);
    callback.in(out);
    return position;
  }

  default Function<I, O> toFunction() {
    return this::io;
  }

  static <I, O> In1Out1<I, O> of(In1Out1<I, O> lambda) {
    return lambda;
  }

  static <I, O> In1Out1<I, O> of(In1<I> in, Out1<O> out) {
    return i-> {
      in.in(i);
      return out.out1();
    };
  }

  /**
   * This method just exists to give you somewhere to create a lambda that will rethrow exceptions.
   */
  static <I, O> In1Out1<I, O> unsafe(In1Out1Unsafe<I, O> of) {
    return of;
  }

  interface In1Out1Unsafe <I, O> extends In1Out1<I, O>, Rethrowable{
    O ioUnsafe(I in) throws Throwable;

    default O io(I in) {
      try {
        return ioUnsafe(in);
      } catch (Throwable e) {
        throw rethrow(e);
      }
    }
  }
}
