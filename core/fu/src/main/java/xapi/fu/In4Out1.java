package xapi.fu;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
@SuppressWarnings("unchecked") // yes, this api will let you do terrible things.  Don't do terrible things.
public interface In4Out1<I1, I2, I3, I4, O> extends Rethrowable, Lambda {

  O io(I1 in1, I2 in2, I3 in3, I4 in4);

  default int accept(int position, In1<O> callback, Object... values) {
    final I1 i1 = (I1) values[position++];
    final I2 i2 = (I2) values[position++];
    final I3 i3 = (I3) values[position++];
    final I4 i4 = (I4) values[position++];
    final O out = io(i1, i2, i3, i4);
    callback.in(out);
    return position;
  }

  static <I1, I2, I3, I4, O> In4Out1<I1, I2, I3, I4, O> of(In4Out1<I1, I2, I3, I4, O> lambda) {
    return lambda;
  }

  static <I1, I2, I3, I4, O> In4Out1<I1, I2, I3, I4, O> of(In4<I1, I2, I3, I4> in, Out1<O> out) {
    return (i1, i2, i3, i4)-> {
      in.in(i1, i2, i3, i4);
      return out.out1();
    };
  }

  /**
   * This method just exists to give you somewhere to create a lambda that will rethrow exceptions.
   */
  static <I1, I2, I3, I4, O> In4Out1<I1, I2, I3, I4, O> unsafe(In4Out1Unsafe<I1, I2, I3, I4, O> of) {
    return of;
  }

  default In3Out1<I2, I3, I4, O> supply1(I1 in1) {
    return (in2, in3, in4)->io(in1, in2, in3, in4);
  }

  default In3Out1<I1, I3, I4, O> supply2(I2 in2) {
    return (in1, in3, in4)->io(in1, in2, in3, in4);
  }

  default In3Out1<I1, I2, I4, O> supply3(I3 in3) {
    return (in1, in2, in4)->io(in1, in2, in3, in4);
  }

  default In3Out1<I1, I2, I3, O> supply4(I4 in4) {
    return (in1, in2, in3)->io(in1, in2, in3, in4);
  }

  default Out1<O> supply(I1 in1, I2 in2, I3 in3, I4 in4) {
    return supply1(in1).supply1(in2).supply1(in3).supply(in4);
  }

  static <I1, I2, I3, I4, O> In3Out1<I2, I3, I4, O> with1(In4Out1<I1, I2, I3, I4, O> io, I1 in1) {
    return (in2, in3, in4) -> io.io(in1, in2, in3, in4);
  }

  static <I1, I2, I3, I4, O> In3Out1<I1, I3, I4, O> with2(In4Out1<I1, I2, I3, I4, O> io, I2 in2) {
    return (in1, in3, in4) -> io.io(in1, in2, in3, in4);
  }

  static <I1, I2, I3, I4, O> In3Out1<I1, I2, I4, O> with3(In4Out1<I1, I2, I3, I4, O> io, I3 in3) {
    return (in1, in2, in4) -> io.io(in1, in2, in3, in4);
  }

  static <I1, I2, I3, I4, O> In3Out1<I1, I2, I3, O> with4(In4Out1<I1, I2, I3, I4, O> io, I4 in4) {
    return (in1, in2, in3) -> io.io(in1, in2, in3, in4);
  }

  interface In4Out1Unsafe <I1, I2, I3, I4, O> extends In4Out1<I1, I2, I3, I4, O> {
    O ioUnsafe(I1 i1, I2 i2, I3 i3, I4 i4) throws Throwable;

    default O io(I1 i1, I2 i2, I3 i3, I4 i4) {
      try {
        return ioUnsafe(i1, i2, i3, i4);
      } catch (Throwable e) {
        throw rethrow(e);
      }
    }
  }

}
