package xapi.fu;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public interface OutMany extends Rethrowable {

  Out1[] out0();

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
