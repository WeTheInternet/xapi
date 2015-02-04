package xapi.components.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;

public class JsFunctionAccessors {

  protected static native JavaScriptObject getter(String paramName)
  /*-{
    return function() {
      return this[paramName];
    };
  }-*/;

  protected static native JavaScriptObject setter(String paramName)
  /*-{
    return function(i) {
      return this[paramName] = i == null ? null : i;
    };
  }-*/;

  protected static native JavaScriptObject attributeSetter(String key, JavaScriptObject func)
  /*-{
    return function(i) {
      var val = i == null ? null : func(i);
      if (val == null) {
        this.removeAttribute(key);
      } else {
        this.setAttribute(key, val);
      }
    }
  }-*/;

  protected static native JavaScriptObject attributeGetBoolean(String key)
  /*-{
    return function() {
      return @xapi.components.impl.JsSupport::unboxBoolean(Lcom/google/gwt/core/client/JavaScriptObject;)(this.getAttribute(key));
    }
  }-*/;

  protected static native JavaScriptObject attributeSetBoolean(String key)
  /*-{
    return @xapi.components.impl.JsFunctionAccessors::attributeSetter(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)
    (key, @xapi.components.impl.JsSupport::unboxBoolean(Lcom/google/gwt/core/client/JavaScriptObject;));
  }-*/;

  protected static native JavaScriptObject attributeGetByte(String key)
  /*-{
    return function() {
      return @xapi.components.impl.JsSupport::unboxByte(Lcom/google/gwt/core/client/JavaScriptObject;)(this.getAttribute(key));
    }
  }-*/;

  protected static native JavaScriptObject attributeSetByte(String key)
  /*-{
    return @xapi.components.impl.JsFunctionAccessors::attributeSetter(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)
    (key, @xapi.components.impl.JsSupport::unboxByte(Lcom/google/gwt/core/client/JavaScriptObject;));
  }-*/;

  protected static native JavaScriptObject attributeGetShort(String key)
  /*-{
    return function() {
      return @xapi.components.impl.JsSupport::unboxShort(Lcom/google/gwt/core/client/JavaScriptObject;)(this.getAttribute(key));
    }
  }-*/;

  protected static native JavaScriptObject attributeSetShort(String key)
  /*-{
    return @xapi.components.impl.JsFunctionAccessors::attributeSetter(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)
    (key, @xapi.components.impl.JsSupport::unboxShort(Lcom/google/gwt/core/client/JavaScriptObject;));
  }-*/;

  protected static native JavaScriptObject attributeGetChar(String key)
  /*-{
    return function() {
      return @xapi.components.impl.JsSupport::unboxCharacter(Lcom/google/gwt/core/client/JavaScriptObject;)(this.getAttribute(key));
    }
  }-*/;

  protected static native JavaScriptObject attributeSetChar(String key)
  /*-{
    return @xapi.components.impl.JsFunctionAccessors::attributeSetter(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)
    (key, @xapi.components.impl.JsSupport::unboxCharacter(Lcom/google/gwt/core/client/JavaScriptObject;));
  }-*/;

  protected static native JavaScriptObject attributeGetInt(String key)
  /*-{
    return function() {
      return @xapi.components.impl.JsSupport::unboxInteger(Lcom/google/gwt/core/client/JavaScriptObject;)(this.getAttribute(key));
    }
  }-*/;

  protected static native JavaScriptObject attributeSetInt(String key)
  /*-{
    return @xapi.components.impl.JsFunctionAccessors::attributeSetter(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)
    (key, @xapi.components.impl.JsSupport::unboxInteger(Lcom/google/gwt/core/client/JavaScriptObject;));
  }-*/;

  @UnsafeNativeLong
  protected static native JavaScriptObject attributeGetLong(String key)
  /*-{
    return function() {
      return @xapi.components.impl.JsSupport::unboxLong(Lcom/google/gwt/core/client/JavaScriptObject;)(this.getAttribute(key));
    }
  }-*/;

  @UnsafeNativeLong
  protected static native JavaScriptObject attributeSetLong(String key)
  /*-{
    return @xapi.components.impl.JsFunctionAccessors::attributeSetter(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)
    (key, @xapi.components.impl.JsSupport::unboxLong(Lcom/google/gwt/core/client/JavaScriptObject;));
  }-*/;

  protected static native JavaScriptObject attributeGetFloat(String key)
  /*-{
    return function() {
      return @xapi.components.impl.JsSupport::unboxFloat(Lcom/google/gwt/core/client/JavaScriptObject;)(this.getAttribute(key));
    }
  }-*/;

  protected static native JavaScriptObject attributeSetFloat(String key)
  /*-{
    return @xapi.components.impl.JsFunctionAccessors::attributeSetter(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)
    (key, @xapi.components.impl.JsSupport::unboxFloat(Lcom/google/gwt/core/client/JavaScriptObject;));
  }-*/;

  protected static native JavaScriptObject attributeGetDouble(String key)
  /*-{
    return function() {
      return @xapi.components.impl.JsSupport::unboxDouble(Lcom/google/gwt/core/client/JavaScriptObject;)(this.getAttribute(key));
    }
  }-*/;

  protected static native JavaScriptObject attributeSetDouble(String key)
  /*-{
    return @xapi.components.impl.JsFunctionAccessors::attributeSetter(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)
    (key, @xapi.components.impl.JsSupport::unboxDouble(Lcom/google/gwt/core/client/JavaScriptObject;));
  }-*/;

  protected static native JavaScriptObject attributeGetArrayOfString(String key)
  /*-{
    return function() {
      return @xapi.components.impl.JsSupport::unboxArrayOfString(Lcom/google/gwt/core/client/JavaScriptObject;Ljava/lang/String;)(this.getAttribute(key), this.joiner);
    }
  }-*/;

  protected static native JavaScriptObject attributeSetArrayOfString(String key)
  /*-{
    return function(i) {
      var val = i == null ? null : @xapi.components.impl.JsSupport::boxArray(Lcom/google/gwt/core/client/JavaScriptObject;Ljava/lang/String;)(i, i && i.joiner);
      if (val == null) {
        this.removeAttribute(key);
      } else {
        this.setAttribute(key, val);
      }
    }
  }-*/;

  protected static native JavaScriptObject getterBoolean(String key)
  /*-{
    return function() {
      return @xapi.components.impl.JsSupport::unboxBoolean(Lcom/google/gwt/core/client/JavaScriptObject;)(this[key]);
    }
  }-*/;

  protected static native JavaScriptObject setterBoolean(String key) /*-{
    return function(i) {
      this[key] = i == null ? null : @xapi.components.impl.JsSupport::unboxBoolean(Lcom/google/gwt/core/client/JavaScriptObject;)(i);
      return this;
    }
  }-*/;

  protected static native JavaScriptObject getterByte(String key)
  /*-{
    return function() {
      return @xapi.components.impl.JsSupport::unboxByte(Lcom/google/gwt/core/client/JavaScriptObject;)(this[key]);
    }
  }-*/;

  protected static native JavaScriptObject setterByte(String key) /*-{
    return function(i) {
      this[key] = i == null ? null : @xapi.components.impl.JsSupport::unboxByte(Lcom/google/gwt/core/client/JavaScriptObject;)(i);
      return this;
    }
  }-*/;

  protected static native JavaScriptObject getterShort(String key)
  /*-{
    return function() {
      return @xapi.components.impl.JsSupport::unboxShort(Lcom/google/gwt/core/client/JavaScriptObject;)(this[key]);
    }
  }-*/;

  protected static native JavaScriptObject setterShort(String key) /*-{
    return function(i) {
      this[key] = i == null ? null : @xapi.components.impl.JsSupport::unboxShort(Lcom/google/gwt/core/client/JavaScriptObject;)(i);
      return this;
    }
  }-*/;

  protected static native JavaScriptObject getterChar(String key)
  /*-{
    return function() {
      return @xapi.components.impl.JsSupport::unboxCharacter(Lcom/google/gwt/core/client/JavaScriptObject;)(this[key]);
    }
  }-*/;

  protected static native JavaScriptObject setterChar(String key) /*-{
    return function(i) {
      this[key] = i == null ? null : @xapi.components.impl.JsSupport::unboxCharacter(Lcom/google/gwt/core/client/JavaScriptObject;)(i);
      return this;
    }
  }-*/;

  protected static native JavaScriptObject getterInt(String key)
  /*-{
    return function() {
      return @xapi.components.impl.JsSupport::unboxInteger(Lcom/google/gwt/core/client/JavaScriptObject;)(this[key]);
    }
  }-*/;

  protected static native JavaScriptObject setterInt(String key) /*-{
    return function(i) {
      this[key] = i == null ? null : @xapi.components.impl.JsSupport::unboxInteger(Lcom/google/gwt/core/client/JavaScriptObject;)(i);
      return this;
    }
  }-*/;

  @UnsafeNativeLong
  protected static native JavaScriptObject getterLong(String key)
  /*-{
    return function() {
      return @xapi.components.impl.JsSupport::unboxLong(Lcom/google/gwt/core/client/JavaScriptObject;)(this[key]);
    }
  }-*/;

  @UnsafeNativeLong
  protected static native JavaScriptObject setterLong(String key) /*-{
    return function(i) {
      this[key] = i == null ? null : @xapi.components.impl.JsSupport::unboxLong(Lcom/google/gwt/core/client/JavaScriptObject;)(i);
      return this;
    }
  }-*/;

  @UnsafeNativeLong
  protected static native JavaScriptObject getterFloat(String key)
  /*-{
    return function() {
      return @xapi.components.impl.JsSupport::unboxFloat(Lcom/google/gwt/core/client/JavaScriptObject;)(this[key]);
    }
  }-*/;

  protected static native JavaScriptObject setterFloat(String key) /*-{
    return function(i) {
      this[key] = i == null ? null : @xapi.components.impl.JsSupport::unboxFloat(Lcom/google/gwt/core/client/JavaScriptObject;)(i);
      return this;
    }
  }-*/;

  protected static native JavaScriptObject getterDouble(String key)
  /*-{
    return function() {
      return @xapi.components.impl.JsSupport::unboxDouble(Lcom/google/gwt/core/client/JavaScriptObject;)(this[key]);
    }
  }-*/;

  protected static native JavaScriptObject setterDouble(String key) /*-{
    return function(i) {
      this[key] = i == null ? null : @xapi.components.impl.JsSupport::unboxDouble(Lcom/google/gwt/core/client/JavaScriptObject;)(i);
      return this;
    }
  }-*/;

  protected static native JavaScriptObject entry(JavaScriptObject func)
  /*-{
    return $entry(function(){
      var ret = func(this);
      return ret;
    });
  }-*/;

  protected static native JavaScriptObject entryWithArgs(JavaScriptObject func)
  /*-{
    return $entry(function(){
      var ret = func.apply(this, [this].concat(Array.prototype.slice.apply(arguments)));
      return ret;
    });
  }-*/;

}
