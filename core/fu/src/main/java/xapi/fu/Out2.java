package xapi.fu;

import xapi.fu.Out1.Out1Unsafe;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
@SuppressWarnings("unchecked")
public interface Out2<O1, O2> extends OutMany {

  default O1 out1() {
    return out1Provider().out1();
  }

  default O2 out2() {
    return out2Provider().out1();
  }

  default Out1<O1> out1Provider() {
    return (Out1<O1>) out0()[0];
  }

  default Out1<O2> out2Provider() {
    return (Out1<O2>) out0()[1];
  }

  default Out2<O1, O2> use1(In1<O1> callback) {
    callback.in(out1());
    return this;
  }

  default Out2<O1, O2> use2(In1<O1> callback) {
    callback.in(out1());
    return this;
  }

  /**
   * @return an immutable copy of this object.
   */
  default <F extends Out2<O1, O2> & Frozen> F freeze() {
    if (this instanceof Frozen) {
      return (F) this;
    }
    final Out1[] outs = out0();
    outs[0] = outs[0].freeze();
    outs[1] = outs[1].freeze();
    F f = (F)(Out2<O1, O2> & Frozen)()->outs;
    return f;
  }

  static <O1, O2> Out2<O1, O2> out2(Out1<O1> o1, Out1<O2> o2) {
    Out1[] out = new Out1[]{o1, o2};
    return ()->out;
  }

  static <O1, O2> Out2<O1, O2> out2(O1 o1, Out1<O2> o2) {
    Out1[] out = new Out1[]{Out1.out1(o1), o2};
    return ()->out;
  }

  static <O1, O2> Out2<O1, O2> out2(O1 o1, O2 o2) {
    Out1[] out = new Out1[]{Out1.out1(o1), Out1.out1(o2)};
    return ()->out;
  }

  static <O1, O2> Out2<O1, O2> out2(Out1<O1> o1, O2 o2) {
    Out1[] out = new Out1[]{o1, Out1.out1(o2)};
    return ()->out;
  }

  static <O1, O2> Out2<O1, O2> out2Unsafe(O1 o1, Out1Unsafe<O2> o2) {
    return out2(o1, o2);
  }

  static <O1, O2> Out2<O1, O2> out2Unsafe(Out1Unsafe<O1> o1, Out1Unsafe<O2> o2) {
    return out2(o1, o2);
  }

  static <O1, O2> Out2<O1, O2> out2Unsafe(Out1Unsafe<O1> o1, O2 o2) {
    return out2(o1, o2);
  }

}
