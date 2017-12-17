package xapi.components.impl;

import elemental.dom.Element;
import elemental.js.dom.JsElement;
import xapi.components.api.ComponentNamespace;
import xapi.components.api.Document;
import xapi.components.api.IsWebComponent;
import xapi.components.api.JsoConstructorSupplier;
import xapi.fu.Do;
import xapi.fu.In1Out1;
import xapi.fu.In2Out1;
import xapi.fu.Mutable;
import xapi.gwt.api.JsLazyExpando;
import xapi.ui.api.component.ComponentConstructor;
import xapi.ui.api.component.ComponentOptions;
import xapi.ui.api.component.IsComponent;
import xapi.ui.api.component.IsGraphComponent;

import javax.validation.constraints.NotNull;
import java.util.function.Supplier;

import static xapi.components.api.ComponentNamespace.findParentComponent;
import static xapi.components.api.LoggingCallback.voidCallback;
import static xapi.components.impl.JsSupport.setFactory;
import static xapi.components.impl.WebComponentVersion.V0;
import static xapi.components.impl.WebComponentVersion.V1;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.ScriptInjector;

public class WebComponentSupport {

  public static final WebComponentVersion VERSION = "true".equals(
      System.getProperty("web.components.v0", "false")) ? V0 : V1;

    private static final In1Out1<Element, ? extends IsComponent<Element>> EXPECT_INITIALIZED = ignored -> {
        throw new RuntimeException("Attached component not found");
    };

    @SuppressWarnings("unchecked")
    public static
            <
                E extends Element,
                Me extends IsComponent<E>
            >
    In1Out1<E, Me> installFactory(
        WebComponentBuilder b,
        In1Out1<E, Me> defaultFactory,
        ComponentOptions<E, Me> options
    ) {

        JsLazyExpando<E, Me> expando = new JsLazyExpando<>(ComponentNamespace.JS_KEY);
        // This sets up a provider on the prototype of the element.
        // When the element is created in javascript / via innerHTML,
        // any attempt to access the component will route through here,
        // which will either use the factory in the supplied default options,
        // or we use the defaultFactory, which will call a component constructor
        expando.addToPrototype(b.getPrototype(), e->{
            Me result;
            if (options.needsComponent()) {
                result = defaultFactory.io(e);
            } else {
                result = options.newComponent(e);
            }
            expando.setValue(e, result);
            return result;
        }, false);
        // The component expando is a bit special,
        // since other created callbacks likely want to get the component instance,
        // we need to pre-emptively set it in the constructor when a component is supplied.
        b.<E, Me>createdCallback((e, opts) -> {
            final Me result;
            if (opts != null && opts.hasExisting()) {
                result = opts.getExisting();
                expando.setValue(e, result);
//            } else {
//                expando.setValue(e, defaultFactory.io(e));
            }
        });

        b.attachedCallback(e->{
            final Element parentComponent = findParentComponent(e);
            if (parentComponent != null) {
                final IsComponent parent = ComponentNamespace.getComponent(parentComponent, EXPECT_INITIALIZED);
                final IsComponent me = ComponentNamespace.getComponent(e, EXPECT_INITIALIZED);
                if (me instanceof IsGraphComponent) {
                    ((IsGraphComponent)me).setParentComponent(parent);
                }
                if (parent instanceof IsGraphComponent) {
                    ((IsGraphComponent)parent).addChildComponent(me);
                }
            }
        });
        return In2Out1.with2(ComponentNamespace::getComponent, defaultFactory);
    }

    public static void installComponent(
        Element e, IsComponent<?> component
    ) {
        setFactory(e, component, ComponentNamespace.JS_KEY);
    }

    public void ensureWebComponentApi(Do onLoaded) {
    // Check if document.register exists

    if (!hasNativeSupport()) {
      // Nope... Lets inject our polyfill
      Mutable<JavaScriptObject> script = new Mutable<>();
      script.in(ScriptInjector
          .fromUrl(getPolyfillUrl(VERSION))
          .setCallback(voidCallback(() -> {
            if (VERSION == V1) {
              JsSupport.win().addEnteredListener("WebComponentsReady", onLoaded.onlyOnce().ignores1()::in, false);
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

  public static boolean hasNativeSupport() {
      if (VERSION == V0) {
          return JsSupport.doc().getRegisterElement() != null;
      }
      return JsSupport.customElements() != null;
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
//      return GWT.getModuleBaseURL() + "js/webcomponents-v1.js";
      return GWT.getModuleBaseURL() + "js/components-v1.js";
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

  public static <E, C extends IsComponent<E>> ComponentConstructor<E, C> define(String tagName, JavaScriptObject jsClass, String extendsTag) {
    ComponentConstructor jso = JsSupport.defineTag(tagName, jsClass, JsSupport.extendsTag(extendsTag));
    return jso;
  }

  public static <E, C extends IsComponent<E>> ComponentConstructor<E, C> define(String tagName, WebComponentBuilder component) {
    ComponentConstructor jso = JsSupport.defineTag(tagName, component.getComponentClass(), JsSupport.extendsTag(component.getSuperTag()));
    return jso;
  }
}
