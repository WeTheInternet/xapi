package xapi.components.api;

import com.google.gwt.core.client.js.JsType;

@JsType
public interface OnWebComponentAttributeChanged extends WebComponentCallback {

  @NativelySupported
  void onAttributeChanged(String name, String oldVal, String newVal);

}
