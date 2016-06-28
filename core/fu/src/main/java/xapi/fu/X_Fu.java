package xapi.fu;

import static xapi.fu.MappableIterable.mappable;

import java.util.Iterator;
import java.util.function.Predicate;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public interface X_Fu {

  In1Out1<String, String> STRING_DUPLICATE = s -> s + s;

  static <T> T[] array(T... t) {
    return t;
  }

  static <T> T[] push(T[] ts, T t) {
    return Fu.jutsu.pushOnto(ts, t);
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

  static <T> Filter<T> alwaysTrue() {
    return X_Fu::returnTrue;
  }

  static <T> Filter<T> alwaysFalse() {
    return X_Fu::returnFalse;
  }

  static <T> String reduceToString(Iterable<T> data, In1Out1<T, String> serializer, String separator) {
    return reduceToString(map(data, serializer), separator);
  }

  static <F, T> Iterable<T> map(Iterable<F> data, In1Out1<F, T> converter) {
    return mappable(data).map(converter);
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

}

