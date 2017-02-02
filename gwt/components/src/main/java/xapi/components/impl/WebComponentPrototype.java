package xapi.components.impl;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import xapi.components.api.JsoArray;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/24/16.
 */
@JsType(isNative = true)
interface WebComponentPrototype {

  @JsProperty
  void setAttachedCallback(JavaScriptObject callback);

  @JsProperty
  void setConnectedCallback(JavaScriptObject callback);

  @JsProperty
  JavaScriptObject getAttachedCallback();

  @JsProperty
  JavaScriptObject getConnectedCallback();

  @JsProperty
  void setCreatedCallback(JavaScriptObject callback);

  @JsProperty
  JavaScriptObject getCreatedCallback();

  @JsProperty
  /**
   * The actual name in the js class must be constructor,
   * but we do special things with it when building the class,
   * then defer to the optional init method (defined here, as a created callback).
   */
  void setInit(JavaScriptObject callback);

  @JsProperty
  JavaScriptObject getInit();

  @JsProperty
  void setDetachedCallback(JavaScriptObject callback);

  @JsProperty
  JavaScriptObject getDetachedCallback();

  @JsProperty
  void setDisconnectedCallback(JavaScriptObject callback);

  @JsProperty
  JavaScriptObject getDisconnectedCallback();

  @JsProperty
  void setAttributeChangedCallback(JavaScriptObject callback);

  @JsProperty
  JavaScriptObject getAttributeChangedCallback();

  @JsProperty
  void setObservedAttributes(JsoArray<String> callback);

  @JsProperty
  JsoArray<String> getObservedAttributes();

  @JsProperty
  void setAdoptedCallback(JavaScriptObject callback);

  @JsProperty
  JavaScriptObject getAdoptedCallback();
}
