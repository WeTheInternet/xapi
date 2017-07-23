package xapi.components.api;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;
import jsinterop.base.Js;
import xapi.components.impl.JsSupport;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/16/17.
 */
@JsType(isNative = true)
public interface Symbol {

    String getName();

    @JsOverlay
    static Symbol toStringTag() {
        return JsSupport.symbol("toStringTag");
    }

    @JsOverlay
    static boolean isSymbol(Object name) {
        return "symbol".equals(Js.typeof(name));
    }
}
