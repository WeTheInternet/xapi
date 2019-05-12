package xapi.fu;

import xapi.fu.filter.AlwaysFalse;
import xapi.fu.filter.AlwaysTrue;
import xapi.fu.filter.IfNotNull;
import xapi.fu.filter.IfNull;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/14/16.
 */
public interface Filter<T> {

  AlwaysTrue TRUE = new AlwaysTrue();
  IfNotNull IF_NOT_NULL = new IfNotNull();
  IfNull IF_NULL = new IfNull();
  AlwaysFalse FALSE = new AlwaysFalse();

  static <T> Filter1<T> ifNotNull() {
    return IF_NOT_NULL;
  }

  static <T> Filter1<T> ifNull() {
    return IF_NULL;
  }

  static <T> Filter1<T> alwaysTrue() {
    return TRUE;
  }

  static <T> Filter1<T> alwaysFalse() {
    return FALSE;
  }

  boolean filter(T ... args);

  interface Filter1 <T> extends Filter<T> {

    boolean filter1(T item);

    default Filter1<T> inverse() {
      return i->!filter1(i);
    }

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

  interface Filter2 <Super, One extends Super, Two extends Super> extends Filter<Super> {

    boolean filter2(One one, Two two);

    @Override
    @SuppressWarnings("all")
    default boolean filter(Super ... args) {
      for (int i = 0; i < args.length; i+= 2) {
        One one = (One) args[i];
        Two two = (Two) args[i+1];
        if (!filter2(one, two)) {
          return false;
        }
      }
      return true;
    }
  }
  interface Filter2Unsafe <Super, One extends Super, Two extends Super>
      extends Filter2<Super, One, Two>, Rethrowable {
    boolean filter2Unsafe(One one, Two two) throws Exception;

    @Override
    default boolean filter2(One one, Two two) {
      try {
        return filter2Unsafe(one, two);
      } catch (Exception e) {
        throw rethrow(e);
      }
    }

  }

  interface Filter3<Super, One extends Super, Two extends Super, Three extends Super> extends Filter<Super> {

    boolean filter3(One one, Two two, Three three);

    @Override
    default boolean filter(Super ... args) {
      for (int i = 0; i < args.length; i+= 3) {
        One one = (One) args[i];
        Two two = (Two) args[i+1];
        Three three = (Three) args[i+2];
        if (!filter3(one, two, three)) {
          return false;
        }
      }
      return true;
    }
  }
  interface Filter3Unsafe <Super, One extends Super, Two extends Super, Three extends Super>
      extends Rethrowable, Filter3<Super, One, Two, Three> {

    boolean filter3Unsafe(One one, Two two, Three three) throws Exception;

    @Override
    default boolean filter3(One one, Two two, Three three) {
      try {
        return filter3Unsafe(one, two, three);
      } catch (Exception e) {
        throw rethrow(e);
      }
    }

  }

  static <T> Filter1<T> match1(Filter1<T> filter) {
    return filter;
  }

  static <One, Two> Filter2<Object, One, Two> filter2(Filter2<Object, One, Two> filter) {
    return filter;
  }

  static <One, Two, Three> Filter3<Object, One, Two, Three> filter3(Filter3<Object, One, Two, Three> filter) {
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

  static <T> Filter1<T> match1Unsafe(Filter1Unsafe<T> filter) {
    return filter;
  }

  static <Super, One extends Super, Two extends Super> Filter2<Super, One, Two> match2(Filter2<Super, One, Two> filter) {
    return filter;
  }

  static <Super, O extends Super, T extends Super> Filter2<Super, O, T>
  match2Unsafe(Filter2Unsafe<Super, O, T> filter) {
    return filter;
  }

  static <Super, Ono extends Super, Two extends Super, Three extends Super>
  Filter3<Super, Ono, Two, Three>
  match3(Filter3<Super, Ono, Two, Three> filter) {
    return filter;
  }

  static <Super, Ono extends Super, Two extends Super, Three extends Super>
  Filter3<Super, Ono, Two, Three> match3Unsafe(Filter3Unsafe<Super, Ono, Two, Three> filter) {
    return filter;
  }

}
