package xapi.fu;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/14/16.
 */
public interface Filter<T> {

  Filter TRUE = args->true;
  Filter FALSE = args->false;

  boolean filter(T ... args);

  interface Filter1 <T> extends Filter<T> {

    boolean filter1(T item);

    @Override
    default boolean filter(T... args) {
      for (T arg : args) {
        if (!filter1(arg)) {
          return false;
        }
      }
      return true;
    }
  }
  interface Filter1Unsafe <T> extends Filter1<T>, Rethrowable {
    boolean filter1Unsafe(T item) throws Exception;

    @Override
    default boolean filter1(T args) {
      try {
        return filter1Unsafe(args);
      } catch (Exception e) {
        throw rethrow(e);
      }
    }
  }

  interface Filter2 <S, O extends S, T extends S> extends Filter<S> {

    boolean filter2(O one, T two);

    @Override
    default boolean filter(S ... args) {
      for (int i = 0; i < args.length; i+= 2) {
        O one = (O) args[i];
        T two = (T) args[i+1];
        if (!filter2(one, two)) {
          return false;
        }
      }
      return true;
    }
  }
  interface Filter2Unsafe <S, O extends S, T extends S> extends Filter2<S, O, T>, Rethrowable {
    boolean filter2Unsafe(O one, T two) throws Exception;

    @Override
    default boolean filter2(O one, T two) {
      try {
        return filter2Unsafe(one, two);
      } catch (Exception e) {
        throw rethrow(e);
      }
    }

  }

  static <T> Filter1<T> filter1(Filter1<T> filter) {
    return filter;
  }

  static <T> Filter1<T> referenceFilter(T value) {
    return t->t==value;
  }

  static <T> Filter1<T> equalsFilter(T value) {
    if (value == null) {
      return t->t==null;
    }
    return value::equals;
  }

  static <T> Filter1<T> filter1Unsafe(Filter1Unsafe<T> filter) {
    return filter;
  }

  static <S, O extends S, T extends S> Filter2<S, O, T> filter2(Filter2<S, O, T> filter) {
    return filter;
  }

  static <S, O extends S, T extends S> Filter2<S, O, T> filter2Unsafe(Filter2Unsafe<S, O, T> filter) {
    return filter;
  }

  static Filter<Throwable> alwaysTrue() {
    return TRUE;
  }

  static Filter<Throwable> alwaysFalse() {
    return FALSE;
  }
}
