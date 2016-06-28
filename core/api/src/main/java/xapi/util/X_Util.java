package xapi.util;

import xapi.fu.In2Out1;
import xapi.fu.X_Fu;

import static xapi.fu.X_Fu.blank;
import static xapi.util.X_Runtime.isJavaScript;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

/**
 * Generic purpose utility methods;
 * this class has no fields, no
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public final class X_Util{

  private X_Util() {}

  public static boolean equal(final Object one, final Object two){
    return one == two || (one != null && one.equals(two));
  }

  public static <T> T firstNotNull(final T first, final T second) {
    return first == null ? second : first;
  }

  public static <T> T firstNotNull(final T first, final T second, final T third) {
    return first == null ? second == null ? third : second : first;
  }

  @SuppressWarnings("unchecked")
  public static <T> T firstNotNull(final T first, final T ... rest) {
    if (first == null) {
      for (final T t : rest) {
        if (t != null) {
          return t;
        }
      }
    }
    return first;
  }

  public static RuntimeException rethrow(Throwable e) {
    if (e instanceof RuntimeException)
     {
      throw (RuntimeException)e;// Don't re-wrap
    }
    if (e instanceof Error)
     {
      throw ((Error)e);// Just rethrow errors without wrappers
    }
    while (// unwrap checked wrappers, for ease later on
        e instanceof InvocationTargetException
        || e instanceof ExecutionException
        ) {
      if (e.getCause()!=null){
        e = e.getCause();
      } else if (e == e.getCause()) {
        break;
      }
    }
    if (e instanceof InterruptedException) {
      Thread.currentThread().interrupt();
    }
    // throw unchecked.
    throw new RuntimeException(e);
  }

  public static int zeroSafeInt(final Integer i) {
    return i == null ? 0 : i;
  }

  public static double zeroSafeDouble(final Number i) {
    return i == null ? 0 : i.doubleValue();
  }

  public static Throwable unwrap(Throwable e) {
    if (X_Runtime.isDebug()) {
      e.printStackTrace();// Avoid losing information in debug mode.
    }

    //don't use instanceof because we don't want to treat subclasses of RuntimeException as wrappers...
    while (e.getClass().equals(RuntimeException.class) || e.getClass().equals(ExecutionException.class)) {
      if (e.getCause() == null) {
        return e;
      }
      e = e.getCause();
    }
    return e;
  }

  public static <T> T[] pushIfMissing(T[] array, T item) {
      return pushIf(array, item, (b, i)->indexOf(array, item) == -1);
  }

  public static <T> T[] pushAllMissing(T[] array, T ... items) {
      if (items == null || items.length == 0) {
          return array;
      }
      T[] missing = blank(items);
      int cnt = 0;
      for (int i = 0; i < items.length; i++ ) {
          final T item = items[i];
          int index = indexOf(array, items[i]);
          // record missing items
          if (index == -1) {
              // do not double-add items.
              if (indexOf(missing, item) == -1) {
                missing[cnt++] = item;
              }
          }
      }
      if (cnt == 0) {
          return array;
      }
      final T[] result = X_Fu.copy(array, array.length + cnt);
      System.arraycopy(missing, 0, result, array.length, cnt);
      return result;
  }

    public static void main(String ... a) {
        Integer[] is = new Integer[]{1, 2, 3};
        Integer[] missing = new Integer[]{4, 4, 3, 1, 2, 33, 5};
        final Integer[] res = pushAllMissing(is, missing);
        assert res.length == 5;
        assert res[3] == 4;
        assert res[4] == 5;
        System.out.println(Arrays.asList(res));
    }

  public static <T> T[] pushIf(T[] beforeFinished, T t, In2Out1<T[], T, Boolean> filter) {
      if (filter.io(beforeFinished, t)) {
          return pushOnto(beforeFinished, t);
      }
      return beforeFinished;
  }

  public static <T> T[] pushOnto(T[] beforeFinished, T t) {
    if (isJavaScript()) {
      beforeFinished[beforeFinished.length] = t;
      return beforeFinished;
    } else {
      T[] copy = X_Fu.copy(beforeFinished, beforeFinished.length+1);
      copy[beforeFinished.length] = t;
      return copy;
    }
  }

  public static boolean isArray(Object items) {
    if (items == null) {
      return false;
    }
    if (isJavaScript()) {
      return jsniIsArray(items);
    } else {
      return items.getClass().isArray();
    }
  }

  private static native boolean jsniIsArray(Object items)
  /*-{
    return Array.isArray(items);
  }-*/;

    public static <K> int indexOf(K[] from, K match) {
        if (from == null || from.length == 0) {
            return -1;
        }
        if (match == null) {
            for (int i = 0; i < from.length; i++) {
                if (from[i] == null) {
                    return i;
                }
            }
        } else {
            for (int i = 0; i < from.length; i++) {
                if (match.equals(from[i])) {
                    return i;
                }
            }
        }
        return -1;
    }
}
