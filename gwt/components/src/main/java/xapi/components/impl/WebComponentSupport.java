package xapi.components.impl;

import static xapi.components.api.LoggingCallback.voidCallback;

import java.util.function.Supplier;

import javax.validation.constraints.NotNull;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.client.ScriptInjector;

import xapi.components.api.Document;
import xapi.components.api.IsWebComponent;
import xapi.components.api.JsoConstructorSupplier;

import elemental.dom.Element;
import elemental.js.dom.JsElement;

public class WebComponentSupport {

  public void ensureWebComponentApi(ScheduledCommand onLoaded) {
    // Check if document.register exists
    if (JsSupport.doc().registerElement() == null) {
      // Nope... Lets inject our polyfill
      ScriptInjector
        .fromUrl(getPolyfillUrl())
        .setCallback(voidCallback(onLoaded))
        .setRemoveTag(true)
        .inject();
    } else {
      Scheduler.get().scheduleDeferred(onLoaded);
    }
  }

  public static <E extends Element> E asElement(
      @NotNull IsWebComponent<E> webComponent) {
    if (webComponent instanceof JsElement) {
      return castToElement(webComponent);
    }
    throw new RuntimeException("Unknown web component type "
      + webComponent.getClass());
  }

  private static native <E extends Element> E castToElement(
      IsWebComponent<E> webComponent)
  /*-{
		return webComponent;
  }-*/;

  protected String getPolyfillUrl() {
    return GWT.getHostPageBaseURL() + "x-tag-components.min.js";
  }

  public static <E extends IsWebComponent<? extends Element>> Supplier<E> register(String tagName,
      JavaScriptObject build) {
    Document doc = JsSupport.doc();
    JavaScriptObject jso = doc.registerElement().call(doc, tagName, build);
    return new JsoConstructorSupplier<E>(jso);
  }

}
