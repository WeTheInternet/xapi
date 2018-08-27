package xapi.components.api;

import elemental.dom.Node;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;
import jsinterop.base.Js;
import xapi.components.impl.JsSupport;
import xapi.gwt.api.JsObjectDescriptor;
import xapi.gwt.api.Symbol;

@JsType(isNative = true, name = "Object")
public interface JsObject {

    <T> T create(T clone);

    void defineProperty(Object onto, Object name, JsObjectDescriptor properties);

    @JsOverlay
    default Object getProperty(Object name) {
        if (Symbol.isSymbol(name)) {
            return JsSupport.getObject(this, Js.<Symbol>uncheckedCast(name));
        }
        return JsSupport.getObject(this, String.valueOf(name));
    }

    @JsOverlay
    default boolean hasProperty(Object name) {
        return getProperty(name) != null;
    }

    @JsOverlay
    static JsObject cast(Node element) {
        return Js.cast(element);
    }
}
