package xapi.fu;

import java.util.function.Predicate;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public interface X_Fu {


  static <T> T[] array(T ... t) {
    return t;
  }

  static <T> T[] push(T[] ts, T t) {
    return Fu.jutsu.pushOnto(ts, t);
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
    return item->item != null && item.length() > 0;
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

}

/**
 This package-local class implements our platform magic, Jutsu, and initializes it with all default methods.

 This allows you to use techniques like super-sourcing or source-rewriting
 to swap out this interface with your own platform-specific overrides.

 See src/main/resource/xapi/jutsu/xapi/fu/Fu.java for the Gwt implementation of the super-sourcing technique
*/
class Fu implements Jutsu {
  static final Fu jutsu = getFu();

  protected Fu init(Fu jutsu) {
    return jutsu;
  }

  static Fu getFu() {
    final Fu fu = new Fu();
    return fu.init(fu);
  }
}
