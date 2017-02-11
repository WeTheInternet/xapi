package xapi.components.impl;

import elemental.dom.Element;
import elemental.js.dom.JsElement;
import xapi.components.api.ComponentConstructor;
import xapi.components.api.Document;
import xapi.components.api.IsWebComponent;
import xapi.components.api.JsoConstructorSupplier;
import xapi.fu.Do;
import xapi.fu.Mutable;
import xapi.ui.api.component.IsComponent;

import javax.validation.constraints.NotNull;
import java.util.function.Supplier;

import static xapi.components.api.LoggingCallback.voidCallback;
import static xapi.components.impl.WebComponentVersion.V0;
import static xapi.components.impl.WebComponentVersion.V1;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.ScriptInjector;

public class WebComponentSupport {

  public static final WebComponentVersion DEFAULT_VERSION = "true".equals(
      System.getProperty("web.components.v0", "false")) ? V1 : V0;

  public void ensureWebComponentApi(Do onLoaded) {
    ensureWebComponentApi(onLoaded, DEFAULT_VERSION);
  }
  public void ensureWebComponentApi(Do onLoaded, WebComponentVersion version) {
    // Check if document.register exists

    if (JsSupport.doc().getRegisterElement() == null) {
      // Nope... Lets inject our polyfill
      Mutable<JavaScriptObject> script = new Mutable<>();
      script.in(ScriptInjector
          .fromUrl(getPolyfillUrl(version))
          .setCallback(voidCallback(() -> {
            if (version == V1) {
              JsSupport.win().addEventListener("WebComponentsReady", onLoaded.onlyOnce().ignores1()::in, false);
            } else {
              onLoaded.done();
            }
            // TODO: remove the script
          }))
          .setWindow(ScriptInjector.TOP_WINDOW)
          .inject());
    } else {
      Scheduler.get().scheduleDeferred(onLoaded::done);
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

  protected static String getPolyfillUrl(WebComponentVersion version) {
    if (version == V1) {
      return GWT.getModuleBaseURL() + "js/webcomponents-v1.js";
    } else {
      return GWT.getHostPageBaseURL() + "x-tag-components.min.js";
    }
  }

  @Deprecated
  public static <E extends IsWebComponent<? extends Element>> Supplier<E> register(String tagName,
      JavaScriptObject build) {
    Document doc = JsSupport.doc();
    JavaScriptObject jso = doc.getRegisterElement().call(doc, tagName, build);
    return new JsoConstructorSupplier<E>(jso);
  }

  public static <E, C extends IsComponent<E, C>> ComponentConstructor<E, C> define(String tagName, JavaScriptObject jsClass, String extendsTag) {
    ComponentConstructor jso = JsSupport.defineTag(tagName, jsClass, JsSupport.extendsTag(extendsTag));
    return jso;
  }

  public static <E, C extends IsComponent<E, C>> ComponentConstructor<E, C> define(String tagName, WebComponentBuilder component) {
    ComponentConstructor jso = JsSupport.defineTag(tagName, component.getComponentClass(), JsSupport.extendsTag(component.getSuperTag()));
    return jso;
  }
}
