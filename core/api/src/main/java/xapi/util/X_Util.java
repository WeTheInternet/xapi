package xapi.util;


import java.util.Arrays;

import xapi.collect.api.Fifo;
import xapi.collect.impl.SimpleFifo;

/**
 * Generic purpose utility methods;
 * this class has no fields, no 
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public final class X_Util{

  private X_Util() {}
  
  public static String getCaller() {
    Throwable t = new Throwable();
    t.fillInStackTrace();
    Fifo<StackTraceElement> traces = new SimpleFifo<StackTraceElement>();
    traces.giveAll(t.getStackTrace());
    return Arrays.asList(t.getStackTrace()).toString();
  }

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

}
