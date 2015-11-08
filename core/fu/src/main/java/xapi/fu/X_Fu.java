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

interface Fu extends Jutsu {
  Fu jutsu = getFu();
  Fu init();

  static Fu getFu() {
    final Fu fu = () -> jutsu;
    fu.init();
    return fu;
  }
}