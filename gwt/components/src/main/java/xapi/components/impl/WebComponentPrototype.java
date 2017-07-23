package xapi.components.impl;

import elemental2.core.Array;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;
import xapi.components.api.JsoArray;
import xapi.components.api.PropertyConfiguration;

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
   *
   * In addition, we also setup callbacks for afterInit(),
   * which will run automatically, either:
   * whenever first attached to live DOM,
   * in a Scheduler.scheduleFinally (no UI update, only if created from running GWT code),
   * or, at the latest, using RunSoon.schedule (soonest possible callback, but lets UI update).
   *
   * If you need your component to finish initialization earlier,
   * we will mark the tasks attribute of the RunSoon with pending task handles that you should flush
   * (task pids will be space separated).
   */
  void setInit(JavaScriptObject callback);

  @JsProperty
  JavaScriptObject getInit();

  @JsProperty
  void setAfterInit(JavaScriptObject callback);

  @JsProperty
  JavaScriptObject getAfterInit();

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
