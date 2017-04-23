package xapi.components.api;

import elemental.dom.Element;
import xapi.components.impl.JsSupport;
import xapi.fu.In1Out1;
import xapi.ui.api.component.IsComponent;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/8/17.
 */
public class ComponentNamespace {
    public static final Symbol JS_KEY = JsSupport.symbol("ui");

    public static native boolean hasComponent(Object e)
    /*-{
        return !!e[@ComponentNamespace::JS_KEY];
    }-*/;

    public static native <
        E,
        Me extends IsComponent<E, Me>> Me getComponent(E e, In1Out1<E, ? extends Me> getUi)
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
}
