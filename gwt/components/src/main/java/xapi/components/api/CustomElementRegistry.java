package xapi.components.api;

import jsinterop.annotations.JsType;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 1/15/17.
 */
@JsType(isNative = true)
public interface CustomElementRegistry {

    @JsType(isNative = true)
    interface ExtendsTag {

        String getExtends();

        void setExtends(String extendsTag);

    }

    JavaScriptObject define(String name, JavaScriptObject prototype, ExtendsTag extendsTag);

}
