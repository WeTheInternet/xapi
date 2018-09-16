package xapi.fu;

import xapi.fu.iterate.Chain;

import java.lang.reflect.Array;
import java.lang.reflect.Type;

import com.google.gwt.core.shared.GWT;

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

class Fu implements Jutsu {
  static final Fu jutsu = getFu();

  static Fu getFu() {
    // Using GWT.create will allow library authors to inject custom / generated types if they please.
    final Fu fu = GWT.create(Fu.class);
    return fu;
  }

  public <T> T[] emptyArray(T[] notCopied, int length) {
    // This class is not visible on your classpath.  It is from super-source in gwt-dev.jar
    Object arr = javaemul.internal.ArrayHelper.createFrom(notCopied, length);
    return (T[]) arr;
  }

  public String coerce(Object value) {
    return String.valueOf(value);
  }

  public int applyArguments(int i, Chain<HasInput> each, Object ... args) {
    for (HasInput in : each) {
      i = in.accept(i, args);
    }
    return i;
  }

  public <T> T[] pushCopy(T[] ts, T t) {
    T[] result = emptyArray(ts, ts.length + 1);
    System.arraycopy(ts, 0, result, 0, ts.length);
    result[ts.length] = t;
    return result;
  }

  public int[] pushCopy(int[] ts, int t) {
    int[] result = (int[])Array.newInstance(int.class, ts.length + 1);
    System.arraycopy(ts, 0, result, 0, ts.length);
    result[ts.length] = t;
    return result;
  }

  // By default, we always return clones.  Enviros like Gwt can opt to reuse / mutate the array.
  public <T> T[] pushOnto(T[] ts, T t) {
    ts[ts.length] = t;
    return ts;
  }
  public int[] pushOnto(int[] ts, int t) {
    ts[ts.length] = t;
    return ts;
  }

  public Type[] getGenericInterfaces(Class<?> c) {
    if (c == null) {
      return new Type[0];
    }
    Class[] classes = c.getInterfaces();
    Type[] types = new Type[classes.length];
    System.arraycopy(classes, 0, types, 0, classes.length);
    return types;
  }

  public String lambdaName(Object o) {
    return o.getClass().getName();
  }

}
