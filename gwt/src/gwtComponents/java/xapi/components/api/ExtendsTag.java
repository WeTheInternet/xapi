package xapi.components.api;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 2/14/17.
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL)
public interface ExtendsTag {

    @JsProperty
    String getExtends();

    @JsProperty
    void setExtends(String extendsTag);

}
