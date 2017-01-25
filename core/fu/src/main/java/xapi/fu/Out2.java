package xapi.fu;

import xapi.fu.Out1.Out1Unsafe;
import xapi.fu.iterate.Chain;

import static xapi.fu.Immutable.immutable1;

import java.util.Map.Entry;

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

  default Out2<O1, O2> use2(In1<O2> callback) {
    callback.in(out2());
    return this;
  }

  default <To> Out2<O1, To> mapped2(In1Out1<O2, To> mapper) {
    return out2(this::out1, mapper.supplyDeferred(this::out2));
  }

  default <To> Out2<To, O2> mapped1(In1Out1<O1, To> mapper) {
    return out2(mapper.supplyDeferred(this::out1), this::out2);
  }

  /**
   * @return an immutable copy of this object.
   */
  default <F extends Out2<O1, O2> & Frozen> F freeze2() {
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
    final Out1[] out = new Out1[]{o1, o2};
    return ()->out;
  }

  static <O1, O2> Out2<O1, O2> out2(O1 o1, Out1<O2> o2) {
    final Out1[] out = new Out1[]{immutable1(o1), o2};
    return ()->out;
  }

  static <O1, O2> Out2Immutable <O1, O2> out2Immutable(O1 o1, O2 o2) {
    return new Out2Immutable<>(o1, o2);
  }

  class Out2Immutable <O1, O2> implements Out2<O1, O2>, Entry<O1, O2>, IsImmutable {

    private final O1 one;
    private final O2 two;

    public Out2Immutable(O1 one, O2 two) {
      this.one = one;
      this.two = two;
    }

    @Override
    public Out1[] out0() {
      // arrays are mutable, so we give everyone that asks a new one
      return new Out1[]{
          immutable1(one),
          immutable1(two)
      };
    }

    @Override
    public final O1 getKey() {
      return out1();
    }

    @Override
    public final O2 getValue() {
      return out2();
    }

    @Override
    public final O2 setValue(O2 value) {
      throw new UnsupportedOperationException("Object is immutable");
    }

    @Override
    public O1 out1() {
      return one;
    }

    @Override
    public O2 out2() {
      return two;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (!(o instanceof Out2Immutable))
        return false;

      final Out2Immutable<?, ?> that = (Out2Immutable<?, ?>) o;

      if (one != null ? !one.equals(that.one) : that.one != null)
        return false;
      return two != null ? two.equals(that.two) : that.two == null;

    }

    @Override
    public int hashCode() {
      int result = one != null ? one.hashCode() : 0;
      result = 31 * result + (two != null ? two.hashCode() : 0);
      return result;
    }
  }

  static <O1, O2> Out2<O1, O2> out2(Out1<O1> o1, O2 o2) {
    final Out1[] out = new Out1[]{o1, immutable1(o2)};
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

  static <O1, O2> Out2<O1, O2> fromEntry(Entry<O1, O2> entry) {
    return entry instanceof Out2 ?
        (Out2<O1, O2>) entry :
        out2Immutable(entry.getKey(), entry.getValue());
  }

  default Out2<O2, O1> reverse() {
    return out2(out2Provider(), out1Provider());
  }

  static <S, O1 extends S, O2 extends S> MappedIterable<S> iterate(Out2<O1, O2> out) {
    return Chain.<S>startChain()
                .add(out.out1())
                .add(out.out2());
  }

  default String join(String between) {
    return out1() + between + out2();
  }
}
