package xapi.components.api;

import elemental.dom.Element;
import elemental.dom.Node;
import elemental2.core.Reflect;
import jsinterop.base.Js;
import xapi.components.impl.JsSupport;
import xapi.fu.In1Out1;
import xapi.ui.api.component.IsComponent;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/8/17.
 */
public class ComponentNamespace {
    public static final Symbol JS_KEY = JsSupport.symbol("ui");
    public static final Symbol SLOTTER_KEY = JsSupport.symbol("slotter");
    public static final String SHADY_KEY = "shadowController";
    public static final String ATTR_IS_SLOTTED = "xslotted";
    public static final Symbol SHADOW_ROOT_KEY = JsSupport.symbol("shadedRoot");

    public static native boolean hasComponent(Object e)
    /*-{
        return !!e[@ComponentNamespace::JS_KEY];
    }-*/;

    public static native <
        E,
        Me extends IsComponent<? super E, E>> Me getComponent(E e, In1Out1<E, ? extends Me> getUi)
    /*-{
        // you can check if the java class is initialized or not using the map,
        // but should defer to this method to ensure all creation routes through the factory (that you supplied).
        if (e[@ComponentNamespace::JS_KEY]) {
          return e[@ComponentNamespace::JS_KEY];
        }
        var component = getUi.@xapi.fu.In1Out1::io(Ljava/lang/Object;)(e);

        if (!e[@ComponentNamespace::JS_KEY]) {
          // The factory may have defined the property for us, in which case we don't want to try this twice
          Object.defineProperty(e, @ComponentNamespace::JS_KEY, {
            configurable: false, enumerable: false, get: function () {
              return component;
            }
          });
        }
        return component;
    }-*/;

    public static Element findParentComponent(Element e) {
        while (e.getParentNode() != null) {
            e = (Element)e.getParentNode();
            if (JsSupport.exists(e, ComponentNamespace.JS_KEY)) {
                return e;
            }
        }
        return null;
    }

    public static void setSource(Element e, String textContent) {
        Reflect.set(Js.cast(e), "source", textContent);
    }

    public static String getSource(Element element) {
        String source = (String) Reflect.get(Js.cast(element), "source");
        if (source == null) {
            if (element.getChildren().getLength() == 1 && element.getFirstChild().getNodeType() == Node.TEXT_NODE) {
                source = element.getFirstChild().getTextContent();
                setSource(element, source);
            }
        }
        return source;
    }
}
