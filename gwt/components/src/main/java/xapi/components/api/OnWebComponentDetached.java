package xapi.components.api;

import elemental.dom.Element;

public interface OnWebComponentDetached <E extends Element> extends WebComponentCallback {

  @NativelySupported
  void onDetached(E element);

}
