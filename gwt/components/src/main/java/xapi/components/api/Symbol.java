package xapi.components.api;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import jsinterop.base.Js;
import xapi.components.impl.JsSupport;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/16/17.
 */
@JsType(isNative = true, name = "Symbol", namespace = JsPackage.GLOBAL)
public class Symbol {

    public String name;

    @JsOverlay
    public static Symbol toStringTag() {
        return JsSupport.symbol("toStringTag");
    }

    @JsOverlay
    static boolean isSymbol(Object name) {
        return "symbol".equals(Js.typeof(name));
    }

    @JsOverlay
    public final String getName() {
        return name;
    }

    @JsOverlay
    public final void setName(String name) {
        this.name = name;
    }
}
