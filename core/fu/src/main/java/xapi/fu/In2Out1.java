package xapi.fu;

import java.util.function.BiFunction;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
@SuppressWarnings("unchecked") // yes, this api will let you do terrible things.  Don't do terrible things.
public interface In2Out1<I1, I2, O> extends Rethrowable {

  O io(I1 in1, I2 in2);

  default int accept(int position, In1<O> callback, Object... values) {
    final I1 i1 = (I1) values[position++];
    final I2 i2 = (I2) values[position++];
    final O out = io(i1, i2);
    callback.in(out);
    return position;
  }

  default BiFunction<I1, I2, O> toFunction() {
    return this::io;
  }

  static <I1, I2, O> In2Out1<I1, I2, O> of(In2Out1<I1, I2, O> lambda) {
    return lambda;
  }

  static <I1, I2, O> In2Out1<I1, I2, O> of(In2<I1, I2> in, Out1<O> out) {
    return (i1, i2)-> {
      in.in(i1, i2);
      return out.out1();
    };
  }

  /**
   * This method just exists to give you somewhere to create a lambda that will rethrow exceptions.
   */
  static <I1, I2, O> In2Out1<I1, I2, O> unsafe(In2Out1Unsafe<I1, I2, O> of) {
    return of;
  }

  default In1Out1<I2, O> supply1(I1 in1) {
    return in2->io(in1, in2);
  }

  default In1Out1<I1, O> supply2(I2 in2) {
    return in1->io(in1, in2);
  }

  static <I1, I2, O> In1Out1<I2,O> with1(In2Out1<I1, I2, O> io, I1 in1) {
    return in2 -> io.io(in1, in2);
  }

  static <I1, I2, O> In1Out1<I1,O> with2(In2Out1<I1, I2, O> io, I2 in2) {
    return in1 -> io.io(in1, in2);
  }

  interface In2Out1Unsafe <I1, I2, O> extends In2Out1<I1, I2, O> {
    O ioUnsafe(I1 i1, I2 i2) throws Throwable;

    default O io(I1 i1, I2 i2) {
      try {
        return ioUnsafe(i1, i2);
      } catch (Throwable e) {
        throw rethrow(e);
      }
    }
  }
}
