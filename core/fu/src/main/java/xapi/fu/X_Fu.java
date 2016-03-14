package xapi.fu;

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

  static <T> T[] pushCopy(T[] ts, T t) {
    return Fu.jutsu.pushCopy(ts, t);
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
