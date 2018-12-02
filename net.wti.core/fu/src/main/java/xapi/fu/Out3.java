package xapi.fu;

import static xapi.fu.Immutable.immutable1;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
@SuppressWarnings("unchecked")
public interface Out3<O1, O2, O3> extends OutMany {

  default O1 out1() {
    return out1Provider().out1();
  }

  default O2 out2() {
    return out2Provider().out1();
  }

  default O3 out3() {
    return out3Provider().out1();
  }

  default Out1<O1> out1Provider() {
    return (Out1<O1>) out0()[0];
  }

  default Out1<O2> out2Provider() {
    return (Out1<O2>) out0()[1];
  }

  default Out1<O3> out3Provider() {
    return (Out1<O3>) out0()[2];
  }

  default Out3<O1, O2, O3> use1(In1<O1> callback) {
    callback.in(out1());
    return this;
  }

  default Out3<O1, O2, O3> use2(In1<O2> callback) {
    callback.in(out2());
    return this;
  }

  default Out3<O1, O2, O3> use3(In1<O3> callback) {
    callback.in(out3());
    return this;
  }

  /**
   * @return an immutable copy of this object.
   */
  default <F extends Out3<O1, O2, O3> & Frozen> F freeze() {
    if (this instanceof Frozen) {
      return (F) this;
    }
    final Out1[] outs = out0();
    outs[0] = outs[0].freeze();
    outs[1] = outs[1].freeze();
    outs[2] = outs[2].freeze();
    F f = (F)(Out3<O1, O2, O3> & Frozen)()->outs;
    return f;
  }

  static <O1, O2, O3> Out3<O1, O2, O3> out3(Out1<O1> o1, Out1<O2> o2, Out1<O3> o3) {
    final Out1[] out = new Out1[]{o1, o2, o3};
    return ()->out;
  }

  static <O1, O2, O3> Out3<O1, O2, O3> out3(Out1<O1> o1, O2 o2, O3 o3) {
    final Out1[] out = new Out1[]{o1, immutable1(o2), immutable1(o3)};
    return ()->out;
  }

  static <O1, O2, O3> Out3<O1, O2, O3> out3(O1 o1, Out1<O2> o2, O3 o3) {
    final Out1[] out = new Out1[]{immutable1(o1), o2, immutable1(o3)};
    return ()->out;
  }

  static <O1, O2, O3> Out3<O1, O2, O3> out3(O1 o1, O2 o2, Out1<O3> o3) {
    final Out1[] out = new Out1[]{immutable1(o1), immutable1(o2), o3};
    return ()->out;
  }

  static <O1, O2, O3> Out3<O1, O2, O3> out3(O1 o1, Out1<O2> o2, Out1<O3> o3) {
    final Out1[] out = new Out1[]{immutable1(o1), o2, o3};
    return ()->out;
  }

  static <O1, O2, O3> Out3<O1, O2, O3> out3(Out1<O1> o1, O2 o2, Out1<O3> o3) {
    final Out1[] out = new Out1[]{o1, immutable1(o2), o3};
    return ()->out;
  }

  static <O1, O2, O3> Out3<O1, O2, O3> out3(Out1<O1> o1, Out1<O2> o2, O3 o3) {
    final Out1[] out = new Out1[]{o1, o2, immutable1(o3)};
    return ()->out;
  }

  static <O1, O2, O3> Out3<O1, O2, O3> out3(O1 o1, O2 o2, O3 o3) {
    final Out1[] out = new Out1[]{immutable1(o1), immutable1(o2), immutable1(o3)};
    return ()->out;
  }

}
