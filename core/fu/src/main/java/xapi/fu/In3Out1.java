package xapi.fu;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
@SuppressWarnings("unchecked") // yes, this api will let you do terrible things.  Don't do terrible things.
public interface In3Out1<I1, I2, I3, O> extends Rethrowable, Lambda {

  O io(I1 in1, I2 in2, I3 in3);

  default int accept(int position, In1<O> callback, Object... values) {
    final I1 i1 = (I1) values[position++];
    final I2 i2 = (I2) values[position++];
    final I3 i3 = (I3) values[position++];
    final O out = io(i1, i2, i3);
    callback.in(out);
    return position;
  }

  static <I1, I2, I3, O> In3Out1<I1, I2, I3, O> of(In3Out1<I1, I2, I3, O> lambda) {
    return lambda;
  }

  static <I1, I2, I3, O> In3Out1<I1, I2, I3, O> of(In3<I1, I2, I3> in, Out1<O> out) {
    return (i1, i2, i3)-> {
      in.in(i1, i2, i3);
      return out.out1();
    };
  }

  /**
   * This method just exists to give you somewhere to create a lambda that will rethrow exceptions.
   */
  static <I1, I2, I3, O> In3Out1<I1, I2, I3, O> unsafe(In3Out1Unsafe<I1, I2, I3, O> of) {
    return of;
  }

  default In2Out1<I2, I3, O> supply1(I1 in1) {
    return (in2, in3)->io(in1, in2, in3);
  }
  default In2Out1<I2, I3, O> supply1Deferred(Out1<I1> in1) {
    return (in2, in3)->io(in1.out1(), in2, in3);
  }

  default <To> In3Out1<To, I2, I3, O> map1(In1Out1<To, I1> mapper) {
    return (to, i2, i3)->io(mapper.io(to), i2, i3);
  }

  default <To> In3Out1<I1, To, I3, O> map2(In1Out1<To, I2> mapper) {
    return (i1, to, i3)->io(i1, mapper.io(to), i3);
  }

  default <To> In3Out1<I1, I2, To, O> map3(In1Out1<To, I3> mapper) {
    return (i1, i2, to)->io(i1, i2, mapper.io(to));
  }

  default In2Out1<I1, I3, O> supply2(I2 in2) {
    return (in1, in3)->io(in1, in2, in3);
  }

  default In2Out1<I1, I3, O> supply2Deferred(Out1<I2> in2) {
    return (in1, in3)->io(in1, in2.out1(), in3);
  }

  default In2Out1<I1, I2, O> supply3(I3 in3) {
    return (in1, in2)->io(in1, in2, in3);
  }

  default In2Out1<I1, I2, O> supply3Deferred(Out1<I3> in3) {
    return (in1, in2)->io(in1, in2, in3.out1());
  }

  default Out1<O> supply(I1 in1, I2 in2, I3 in3) {
    return supply1(in1).supply1(in2).supply(in3);
  }

  static <I1, I2, I3, O> In2Out1<I2, I3, O> with1(In3Out1<I1, I2, I3, O> io, I1 in1) {
    return (in2, in3) -> io.io(in1, in2, in3);
  }

  static <I1, I2, I3, O> In2Out1<I1, I3, O> with2(In3Out1<I1, I2, I3, O> io, I2 in2) {
    return (in1, in3) -> io.io(in1, in2, in3);
  }

  static <I1, I2, I3, O> In2Out1<I1, I2, O> with3(In3Out1<I1, I2, I3, O> io, I3 in3) {
    return (in1, in2) -> io.io(in1, in2, in3);
  }

  interface In3Out1Unsafe <I1, I2, I3, O> extends In3Out1<I1, I2, I3, O> {
    O ioUnsafe(I1 i1, I2 i2, I3 i3) throws Throwable;

    default O io(I1 i1, I2 i2, I3 i3) {
      try {
        return ioUnsafe(i1, i2, i3);
      } catch (Throwable e) {
        throw rethrow(e);
      }
    }
  }

}
