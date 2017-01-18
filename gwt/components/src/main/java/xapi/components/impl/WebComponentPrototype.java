package xapi.components.impl;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

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
  void setConstructor(JavaScriptObject callback);

  @JsProperty
  JavaScriptObject getConstructor();

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
  void setAdoptedCallback(JavaScriptObject callback);

  @JsProperty
  JavaScriptObject getAdoptedCallback();
}
