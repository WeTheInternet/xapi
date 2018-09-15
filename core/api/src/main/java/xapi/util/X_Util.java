package xapi.util;

import xapi.fu.In2Out1;
import xapi.fu.X_Fu;

import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.ExecutionException;

import static xapi.fu.X_Fu.blank;
import static xapi.util.X_Runtime.isJavaScript;

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
    return
        one == two || // success if pointers match
        (
            one != null && // fail if either pointer is null (would have already succeeded)
            one.equals(two) // succeed if .equals() methods agree
        );
  }

  public static boolean notEqual(final Object one, final Object two){
    return
        one != two && // fail if pointers match
        one != null && // fail if either pointer is null (null would already have failed)
        !one.equals(two); // fail if .equals() method disagrees
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
    while (// unwrap common wrappers, for ease later on
        e instanceof InvocationTargetException
        || e instanceof ExecutionException
        || e instanceof UndeclaredThrowableException
        || e instanceof UncheckedIOException
    ) {
      if (e.getCause()!=null){
        e = e.getCause();
      }
      if (e == e.getCause()) {
        break;
      }
    }
    if (e instanceof InterruptedException) {
      Thread.currentThread().interrupt();
    }
    // throw unchecked. we only claim to return so you can (choose to) write: `throw rethrow(e);`
    throw new RuntimeException(e);
    // TODO: consider using sneakyThrow semantics instead?
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

    //don't use instanceof because we don't want to treat all subclasses of RuntimeException as wrappers...
    //TODO: consider using classname equality, to be kind to people with terrible classloaders.
    final Throwable original = e;
    while (
        e.getClass().equals(RuntimeException.class) ||
        e.getClass().equals(ExecutionException.class) ||
        e.getClass().equals(InvocationTargetException.class) ||
        e.getClass().equals(UndeclaredThrowableException.class)
    ) {
      if (e.getCause() == null) {
        // when unwrapping exceptions,
        // we want to add our parent throwable as a suppressed exception,
        // so we can see the code who originally called us.

        // We set the original throwable as the suppressed exception
        // of the final, non-wrapper throwable that calling code wants to see.

        // This will maintain the deepest level of the call stack,
        // without exponentially increasing log sizes when by
        // adding multiple parent-child suppressed throwable relationships.
        // That is, if you have a cause, we do not add the suppressed original.
        e.addSuppressed(original);
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
      T[] copy = X_Fu.push(beforeFinished, t);
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

    public static void maybeRethrow(Throwable e) {
        if (unwrap(e) instanceof InterruptedException) {
            rethrow(e);
        }
    }

    public static String defaultCharset() {
        return "UTF-8";
    }
}
