package xapi.fu;

import xapi.fu.Filter.Filter2;

import javax.validation.constraints.NotNull;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public interface X_Fu {

  In1Out1<String, String> STRING_DUPLICATE = s -> s + s;
  Object[] EMPTY = new Object[0];

  static <T> T[] array(T... t) {
    return t;
  }

  static <T> T[] push(T[] ts, T t) {
    return Fu.jutsu.pushOnto(ts, t);
  }

  static Object newArray(Class<?> type, int size) {
    return Fu.jutsu.newArray(type, size);
  }

  static Object[] newObjectArray(int size) {
    return new Object[size];
  }

  static Object setArray(Object array, int pos, Object value) {
    Fu.jutsu.setArray(array, pos, value);
    return array;
  }

  static <T> T[] blank(T[] ts) {
    return Fu.jutsu.emptyArray(ts, ts.length);
  }

  static <T> T[] blank(T[] ts, int length) {
    return Fu.jutsu.emptyArray(ts, length);
  }

  static <T> T[] copy(T[] ts, int length) {
    return Fu.jutsu.arrayCopy(ts, length);
  }

  static int[] copy(int[] ts, int length) {
    return Fu.jutsu.arrayCopy(ts, length);
  }

  static int getLength(Object obj) {
    return Fu.jutsu.getLength(obj);
  }

  static void setValue(Object obj, int index, Object value) {
    Fu.jutsu.setValue(obj, index, value);
  }

  static Object getValue(Object obj, int index) {
    return Fu.jutsu.getValue(obj, index);
  }

  static <T> T[] pushCopy(T[] ts, T t) {
    return Fu.jutsu.pushCopy(ts, t);
  }

  static <T extends CharSequence> Predicate<T> notEmpty() {
    return item -> item != null && item.length() > 0;
  }

  static boolean returnTrue() {
    return true;
  }

  static boolean returnFalse() {
    return false;
  }

  static <T> boolean returnTrue(T ignored) {
    return true;
  }

  static <T> boolean returnFalse(T ignored) {
    return false;
  }

  static <T> boolean isNotNull(T t) {
    return t != null;
  }

  static <T> boolean isNull(T t) {
    return t == null;
  }

  static <T> boolean notNull(final T value) {
    return value != null;
  }

  static <T> String reduceToString(Iterable<T> data, In1Out1<T, String> serializer, String separator) {
    return reduceToString(map(data, serializer), separator);
  }

  static <F, T> Iterable<T> map(Iterable<F> data, In1Out1<F, T> converter) {
    return MappedIterable.mapped(data).map(converter);
  }

  static String reduceToString(Iterable<? extends CharSequence> data, String separator) {
    StringBuilder b = new StringBuilder();
    final Iterator<? extends CharSequence> itr = data.iterator();
    if (itr.hasNext()) {
      b.append(itr.next());
      while (itr.hasNext()) {
        b.append(separator);
        b.append(itr.next());
      }
    }
    return b.toString();
  }

  static String getLambdaMethodName(Object o) {
    return Fu.jutsu.lambdaName(o);
  }

  static boolean isLambda(Object o) {
    if (o == null) {
      return false;
    }
    final Class<? extends Object> cl = o.getClass();
    return // cl.isSynthetic() &&  // gwt currently lacks this method :-/
        cl.getName().toLowerCase().contains("$$lambda$");

  }

  static <T> boolean noneNull(final T value, final T value1) {
    return value != null && value1 != null;
  }

  static <T> boolean equal(final T value, final T value1) {

    // cover the most permutations first.
    if (value == value1) {
      // if both were the same reference or null, return quickly
      return true;
    }
    // If either was null, that's a fail.
    if (!noneNull(value, value1)) {
      return false;
    }

    final Class<?> cls = value.getClass();
    final Class<?> cls1 = value1.getClass();
    if (cls == cls1) {
      if (cls.isArray()) {
        // for array types, just iterate items...
        final int len = Fu.jutsu.getLength(value);
        final int len1 = Fu.jutsu.getLength(value1);
        if (len != len1) {
          return false;
        }
        for (int i = 0; i < len; i++) {
          if (!equal(Fu.jutsu.getValue(value, i), Fu.jutsu.getValue(value1, i))) {
            return false;
          }
        }
        return true;
      }
      // two objects with the same class, just evaluate equality
      return value.equals(value1);
    }
    if (cls.isAssignableFrom(cls1)) {
      if (areComparable(value, value1)) {
        // We'll let comparable objects return 0 for us to short-circuit equals().
        return ((Comparable)value).compareTo(value1) == 0;
      }

      if (value instanceof Collection) {
        // both are collections. Check the size() before we check elements.
        int mySize = ((Collection)value).size();
        int yourSize = ((Collection)value1).size();
        if (mySize != yourSize) {
          return false;
        }
      }
      if (value instanceof Map) {
        // both are maps. Check the size() before we check elements.
        int mySize = ((Map)value).size();
        int yourSize = ((Map)value1).size();
        if (mySize != yourSize) {
          return false;
        }
      }
      if (value instanceof Iterable) {
        // both are iterable.
        return iterEqual((Iterable)value, (Iterable)value1);
      }
      if (value instanceof Iterator) {
        // both are iterators
        return iterEqual((Iterator)value, (Iterator) value1);
      }
      // let the type that is NOT assignable decide.
      // This allows you to make types that can "dominate" other types equality methods.
      // For example, an object that knows how to compare itself to a String...
      // Strings will still only compare to each other, but your non-string object can do
      // something other than .toString() when it is being checked.
      return value1.equals(value);
    }
    // in case one of the values is not assignable to the other, we allow them a chance to call .equals();
    if (cls1.isAssignableFrom(cls)) {
      // let the more specific type check equality
      return value.equals(value1);
    }
    // don't even check objects that aren't assignable in at least one direction.
    return false;
  }

  static <T> boolean areComparable(T value, T value1) {
    final Class<?> comp = comparableClassFor(value);
    if (comp == null) {
      return false;
    }
    final Class<?> comp1 = comparableClassFor(value1);
    return comp == comp1;
  }

  /**
   * Returns the comparable class for the given object,
   * if it is comparable to itself;
   * that is:
   * interface Type extends Comparable&lt;Type> {}.
   * will allow Type.class to be returned.
   *
   * interface Type extends Supertype, Comparable &lt;Supertype> {}
   * will return null, as only directly comparable objects will
   */
    static Class<?> comparableClassFor(Object x) {
      return comparableClassFor(x, Filter.filter2(X_Fu::equal));
    }
    static Class<?> comparableClassFor(Object x, Filter2<Object, Type, Class<?>> filter) {

      if (x instanceof Comparable) {
        final Class<?> c;
        Type[] genericInterfaces, actualArgs;
        Type type;
        ParameterizedType p;
        if ((c = x.getClass()) == String.class) {
          return c;
        }
        if ((genericInterfaces = Fu.jutsu.getGenericInterfaces(c)) != null) {
          for (int i = 0; i < genericInterfaces.length; ++i) {
            if (((type = genericInterfaces[i]) instanceof ParameterizedType) &&
                ((p = (ParameterizedType)type).getRawType() ==
                    Comparable.class)
                ) {

                if ((actualArgs = p.getActualTypeArguments()) != null &&
                actualArgs.length == 1 && filter.filter2(actualArgs[0], c)) {
                  // type arg is c
                  return c;
                }
            }
            // short circuit if a Comparable match
            return null;
          }
        }
      }
      return null;
    }

  static boolean iterEqual(Iterable v1, Iterable v2) {
    if (v1 == null) {
      return v2 == null;
    }
    if (v2 == null) {
      return false;
    }
    return iterEqualNullsafe(v1.iterator(), v2.iterator());
  }

  static boolean iterEqual(Iterator v1, Iterator v2) {
    if (v1 == null) {
      return v2 == null;
    }
    if (v2 == null) {
      return false;
    }
    return iterEqualNullsafe(v1, v2);
  }
  static boolean iterEqualNullsafe(@NotNull Iterator v1, @NotNull Iterator v2) {
    while (v1.hasNext()) {
      if (!v2.hasNext()) {
        return false;
      }
      if (!equal(v1.next(), v2.next())) {
        return false;
      }
    }
    return true;
  }

  static <I, O> O mapIfNotNull(I in, In1Out1<I, O> mapper) {
    if (in == null) {
      return null;
    }
    return mapper.io(in);
  }

  static <T> T getZeroeth(T[] value) {
    return getNth(value, 0);
  }

  static <T> void setZeroeth(T[] values, T value) {
    setNth(values, value, 0);
  }

  static <T> T putZeroeth(T[] values, T value) {
    return putNth(values, value, 0);
  }

  static <T> T getNth(T[] value, int n) {
    return value[n];
  }

  static <T> void setNth(T[] values, T value, int n) {
    assert values.length < n && n >= 0;
    values[n] = value;
  }

  static <T> T putNth(T[] values, T value, int n) {
    assert values.length < n && n >= 0;
    final T current = values[n];
    values[n] = value;
    return current;
  }

  static boolean isNotZeroed(int[] format) {
    return !isZeroed(format);
  }


  static boolean isZeroed(int[] format) {
    for (int i : format) {
      if (i != 0) {
        return false;
      }
    }
    return true;
  }

  static boolean hasNoNulls(Object[] format) {
    return !hasNulls(format);
  }


  static boolean hasNulls(Object[] format) {
    for (Object i : format) {
      if (i == null) {
        return true;
      }
    }
    return false;
  }

  static boolean isEmpty(Object[] format) {
    return format == null || format.length == 0;
  }

  static boolean isNotEmpty(Object[] format) {
    return !isEmpty(format);
  }

  static int minusOne(int val) {
    return val-1;
  }

  static int plusOne(int val) {
    return val+1;
  }

  static Integer nullSafeIncrement(Integer in) {
    return in == null ? 0 : in + 1;
  }

  @SuppressWarnings("unchecked")
  static <T> T[] emptyArray() {
    return (T[]) EMPTY;
  }

  static int increment(int integer) {
    return integer + 1;
  }

  static int decrement(int integer) {
    return integer - 1;
  }
}

