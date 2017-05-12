package xapi.fu;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

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

  default <T> T[] arrayCopy(T[] copied, int from, int length) {
    T[] arr = emptyArray(copied, length);
    System.arraycopy(copied, from, arr, 0, Math.min(length, copied.length));
    return arr;
  }

  default int[] arrayCopy(int[] copied, int length) {
    int[] arr = new int[length];
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

  default String lambdaName(Object o) {
    final Class<?> c = o.getClass();
    Method replaceMethod;
    try {
      replaceMethod = c.getDeclaredMethod("writeReplace");
    } catch (NoSuchMethodException e) {
      return null;
    }
    replaceMethod.setAccessible(true);
    Object lambda;
    try {
      lambda = replaceMethod.invoke(o);
      // the name of the generated lambda method is the perfect identifier for our lambda handlers :-)
      Class l = Class.forName("java.lang.invoke.SerializedLambda");
      Method m = l.getMethod("getImplMethodName");
      String name = (String)m.invoke(lambda);
      if (Boolean.parseBoolean(System.getProperty("xapi.debug", "false")) || !name.contains("lambda")){
        m = l.getMethod("getFunctionalInterfaceClass");
        name = m.invoke(lambda)+ "#" + name;
        m = l.getMethod("getFunctionalInterfaceMethodSignature");
        name = name + m.invoke(lambda);
      }
      m = l.getMethod("getCapturedArgCount");
      int i = (Integer)m.invoke(lambda);
      m = l.getMethod("getCapturedArg", int.class);
      while(i --> 0) {
        name += "|" + System.identityHashCode(m.invoke(lambda, new Integer(i)));
      }
      return name;
    } catch (Exception ignored) {
      ignored.printStackTrace();
    }
    return null;
  }

  default Type[] getGenericInterfaces(Class<?> c) {
    return new Type[0];
  }

  default Object newArray(Class<?> type, int size) {
    return Array.newInstance(type, size);
  }

  default void setArray(Object array, int pos, Object value) {
    Array.set(array, pos, value);
  }
}
