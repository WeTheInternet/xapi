package xapi.fu;

import java.lang.reflect.Array;

/**
 * Jutsu: technique, method, spell, skill or trick.
 * Fujutsu: witchcraft.
 *
 * This package protected class is where any platform-specific magic needs to go,
 * so you can hide things that are not supported on your platform.
 *
 * In order to implement your own Jutsu, you need to look inside the source file of {@link X_Fu},
 * in particular, the package local class {@link Fu}.
 *
 * If you create a copy of this class, along with the {@link Fu#jutsu} field,
 * then everywhere in xapi.fu will use your copy of these "magic operations".
 *
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
interface Jutsu {

  default <T> T[] emptyArray(T[] notCopied, int length) {
    Object arr = Array.newInstance(notCopied.getClass().getComponentType(), length);
    return (T[]) arr;
  }

  default <T> T[] arrayCopy(T[] copied, int length) {
    T[] arr = emptyArray(copied, length);
    System.arraycopy(copied, 0, arr, 0, Math.min(length, copied.length));
    return arr;
  }

  default int getLength(Object obj) {
    return Array.getLength(obj);
  }

  default void setValue(Object obj, int index, Object value) {
    Array.set(obj, index, value);
  }

  default Object getValue(Object obj, int index) {
    return Array.get(obj, index);
  }

  default int applyArguments(int i, Many<HasInput> each, Object ... args) {
    for (HasInput in : each) {
      i = in.accept(i, args);
    }
    return i;
  }

  default <T> T[] pushCopy(T[] ts, T t) {
    T[] result = Fu.jutsu.emptyArray(ts, ts.length + 1);
    System.arraycopy(ts, 0, result, 0, ts.length);
    result[ts.length] = t;
    return result;
  }

  // By default, we always return clones.  Enviros like Gwt can opt to reuse / mutate the array.
  default <T> T[] pushOnto(T[] ts, T t) {
    return pushCopy(ts, t);
  }
}
