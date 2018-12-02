package xapi.fu;

import static xapi.fu.Immutable.immutable1;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public interface OutMany extends Rethrowable, Lambda {

  Out1[] out0();

  default int length() {
    return out0().length;
  }

  static OutMany outMany(Object ... args) {
    Out1[] array = new Out1[args.length];
    for (int i = args.length; i-->0;) {
      Object value = args[i];
      array[i] = immutable1(value);
    }
    return  ()->array;
  }

  static OutMany outManyDeferred(Out1 ... args) {
    return  ()->args;
  }

  /**
   * The expressions sent as arguments will not be evaluated until requested,
   * but will remembered as executed, with frozen values being returned.
   */
  static OutMany outManyLazy(Out1 ... args) {
    Out1[] values = new Out1[args.length];
    for (int i = args.length; i-->0;) {
      Out1 value = args[i];
      int myI = i;
      values[i] = () -> {
         Object v = value.out1();
         values[myI] = immutable1(v);
         return v;
      };
    }
    return  ()->values;
  }

  static OutMany outManyCaching(Filter acceptable, Out1 ... args) {
    Out1[] values = new Out1[args.length];
    for (int i = args.length; i-->0;) {
      Out1 value = args[i];
      int myI = i;
      values[i] = () -> {
         Object v = value.out1();
         if (acceptable.filter(v)) {
           values[myI] = immutable1(v);
         }
         return v;
      };
    }
    return  ()->values;
  }

  static OutMany outManyIntercepted(In1Out1<Out1, Out1> intercept, Out1 ... args) {
    Out1[] values = new Out1[args.length];
    for (int i = args.length; i-->0;) {
      Out1 value = args[i];
      int myI = i;
      values[i] = () -> {
         Object v = intercept.io(value).out1();
         return v;
      };
    }
    return  ()->values;
  }

  static OutMany outManyImmediate(Out1 ... args) {
    Out1[] values = new Out1[args.length];
    for (int i = args.length; i-->0;) {
      Out1 value = args[i];
      values[i] = value.freeze();
    }
    return  ()->values;
  }

  default <T> Out1<T> out(int position) {
    final Out1[] out = out0();
    assert position < out.length : "Bad request; nothing exists @ position "+position+" in "+this;
    final Out1 o = out[position];
    return o;
  }

  default OutMany out(int position, In1 callback) {
    final Out1[] out = out0();
    assert position < out.length : "Bad request; nothing exists @ position "+position+" in "+this;
    final Out1 o = out[position];
    callback.in(o.out1());
    return this;
  }

  default OutMany forAll(In2<Integer, Out1> callback) {
    final Out1[] out = out0();
    for (int i = 0, m = out.length; i < m; i++) {
      callback.in(i, out[i]);
    }
    return this;
  }

  default OutMany forValue(In2<Integer, Object> callback) {
    final Out1[] out = out0();
    for (int i = 0, m = out.length; i < m; i++) {
      callback.in(i, out[i].out1());
    }
    return this;
  }

}
