package xapi.components.api;

import com.google.gwt.core.client.js.JsType;

import elemental.dom.Element;

@JsType
public interface OnWebComponentCreated <E extends Element> extends WebComponentCallback {

  @NativelySupported
  void onCreated(E element);

}
