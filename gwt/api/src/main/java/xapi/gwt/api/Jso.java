package xapi.gwt.api;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import jsinterop.base.Any;
import jsinterop.base.Js;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 12/3/17.
 */
@JsType(isNative = true, name = "Object", namespace = JsPackage.GLOBAL)
public class Jso {

    public static native Any create(Object from);

    @JsMethod
    public static native void defineProperty(Object on, SymbolOrString name, JsObjectDescriptor props);

    @JsOverlay
    public static Any getProperty(Object on, SymbolOrString s) {
        return JsoMagic.getProperty(on, s);
    }

    @JsOverlay
    public static Jso of(Object on) {
        return Js.uncheckedCast(on);
    }

    @JsMethod
    public native boolean hasOwnProperty(SymbolOrString symbolOrString);
}

class JsoMagic {

    static native Any getProperty(Object on, SymbolOrString s)
    /*-{
      return on[s];
    }-*/;

}
