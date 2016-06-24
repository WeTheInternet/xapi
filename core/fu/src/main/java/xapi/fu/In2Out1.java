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

  default Out1<O> supply(I1 in1, I2 in2) {
    return supply1(in1).supply(in2);
  }

  static <I1, I2, O> In1Out1<I2,O> with1(In2Out1<I1, I2, O> io, I1 in1) {
    return in2 -> io.io(in1, in2);
  }

  static <I1, I2, O> In1Out1<I1,O> with2(In2Out1<I1, I2, O> io, I2 in2) {
    return in1 -> io.io(in1, in2);
  }


  /**
   * This hideous looking method enables
   * you to perform inline compute operations,
   * where you can retrieve a value from an object,
   * transform that object into a new value,
   * store that back into the original map,
   * then return the new value.
   *
   * While terrible to read, this method can be used as follows:
   *
   * <pre>
   *   Map<String, Integer> map = new HashMap<>();
   *   map.put("key", 0);
   *   In3Out1.transformCompute(map, Map::get, Map::set)
   *      .io(map, "key", (k, v)->v++);
   *   assert map.get("key") == 2;
   *
   *   When using a Map class, you can use X_Collect.computeMapTransform,
   *   which will supply the Map::get and Map::put method references for you.
   *
   * </pre>
   */
  static <Obj, Key, Val>
  In2Out1<Key , In2Out1<Key, Val, Val>,Val>
  computeKeyValueTransform(Obj obj,
                           In2Out1<Obj, Key, Val> getter,
                           In3<Obj, Key, Val> setter) {
    return (key, t) -> {
      Val v = getter.io(obj, key);
      Val c = t.io(key, v);
      setter.in(obj, key, c);
      return c;
    };
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
