package xapi.util;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

import xapi.util.api.Pair;
import xapi.util.impl.AbstractPair;



/**
 * Generic purpose utility methods;
 * this class has no fields, no 
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public final class X_Util{

  private X_Util() {}
  
  public static boolean equal(Object one, Object two){
    return one == two || (one != null && one.equals(two));
  }

  public static <T> T firstNotNull(T first, T second) {
    return first == null ? second : first;
  }

  public static <T> T firstNotNull(T first, T second, T third) {
    return first == null ? second == null ? third : second : first;
  }

  @SuppressWarnings("unchecked")
  public static <T> T firstNotNull(T first, T ... rest) {
    if (first == null)
    for (T t : rest)
      if (t != null)
        return t;
    return first;
  }

  public static RuntimeException rethrow(Throwable e) {
    if (e instanceof RuntimeException)
      throw (RuntimeException)e;// Don't re-wrap
    if (e instanceof Error)
      throw ((Error)e);// Just rethrow errors without wrappers
    if (// unwrap checked wrappers, for ease later on
        e instanceof InvocationTargetException
        || e instanceof ExecutionException
        )
      if (e.getCause()!=null)e = e.getCause();
    // throw unchecked.  
    throw new RuntimeException(e);
  }

  public static Throwable unwrap(Throwable e) {
    if (X_Runtime.isDebug())
      e.printStackTrace();// Avoid losing information in debug mode.
    
    //don't use instanceof because we don't want to treat subclasses of RuntimeException as wrappers...
    while (e.getClass().equals(RuntimeException.class) || e.getClass().equals(ExecutionException.class)) {
      if (e.getCause() == null)
        return e;
      e = e.getCause();
    }
    return e;
  }

  public static <X, Y> Pair <X, Y> pairOf(X x, Y y) {
    return new AbstractPair<X,Y>(x, y);
  }

  public static <X, Y> Pair <X, Y> newPair() {
    return new AbstractPair<X,Y>();
  }

}
