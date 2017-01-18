package xapi.components.impl;

import elemental.dom.Element;
import elemental.html.DivElement;
import elemental.js.util.JsArrayOfBoolean;
import elemental.js.util.JsArrayOfInt;
import elemental.js.util.JsArrayOfNumber;
import elemental.js.util.JsArrayOfString;
import xapi.components.api.Console;
import xapi.components.api.CustomElementRegistry;
import xapi.components.api.Document;
import xapi.components.api.JsObject;
import xapi.components.api.Symbol;
import xapi.components.api.Window;
import xapi.fu.In1Out1;
import xapi.fu.In2Out1;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;

public class JsSupport {

  private static class NativeFactory <I, O> implements In2Out1<I, In1Out1<I, O>, O> {

    private final Symbol name;

    public NativeFactory(String name) {
      this.name = JsSupport.symbol(name == null ? "__component__" : name);
    }

    @Override
    public native O io(I el, In1Out1<I, O> io)
    /*-{
      var n = this.@NativeFactory::name;
      if (!el[n]) {
        el[n] = io.@In1Out1::io(Ljava/lang/Object;)(el);
      }
      return el[n];
    }-*/;
  }

  public static native Document doc()
  /*-{
  	return $doc;
  }-*/;

  public static native Window win()
  /*-{
  	return $wnd;
  }-*/;

  public static native Symbol symbol(String symbol)
  /*-{
  	return $wnd.Symbol['for'](symbol);
  }-*/;

  public static native CustomElementRegistry customElements()
  /*-{
  	return $wnd.customElements;
  }-*/;

  public static native JsObject object()
  /*-{
  	return Object;
  }-*/;

  public static native Console console()
  /*-{
  	return $wnd.console;
  }-*/;

  public static native Byte boxByte(JavaScriptObject o)
  /*-{
  	return typeof o === 'number' ? @java.lang.Byte::new(B)(~~o)
  			: typeof o === 'string' ? @java.lang.Byte::new(Ljava/lang/String;)(o)
  					: o;
  }-*/;

  public static native Short boxShort(JavaScriptObject o)
  /*-{
  	return typeof o === 'number' ? @java.lang.Short::new(S)(~~o)
  			: typeof o === 'string' ? @java.lang.Short::new(Ljava/lang/String;)(o)
  					: o;
  }-*/;

  public static native Integer boxInteger(JavaScriptObject o)
  /*-{
  	return typeof o === 'number' ? @java.lang.Integer::new(I)(~~o)
  			: typeof o === 'string' ? @java.lang.Integer::new(Ljava/lang/String;)(o)
  					: o;
  }-*/;

  public static native Float boxFloat(JavaScriptObject o)
  /*-{
  	return typeof o === 'number' ? @java.lang.Float::new(F)(o)
  			: typeof o === 'string' ? @java.lang.Float::new(Ljava/lang/String;)(o)
  					: o;
  }-*/;

  public static native Double boxDouble(JavaScriptObject o)
  /*-{
  	return typeof o === 'number' ? @java.lang.Double::new(D)(o)
  			: typeof o === 'string' ? @java.lang.Double::new(Ljava/lang/String;)(o)
  					: o;
  }-*/;

  public static native Character boxCharacter(JavaScriptObject o)
  /*-{
  	return typeof o === 'number' ? @java.lang.Character::new(C)(o) :
  	typeof o === 'string' ? @java.lang.Character::new(C)(o && o.charAt(0) || 0) : o;
  }-*/;

  public static native Boolean boxBoolean(JavaScriptObject o)
  /*-{
  	if (typeof o === 'string') {
  		o = o === 'true';
  	}
  	return typeof o === 'boolean' ? o ? @java.lang.Boolean::TRUE
  			: @java.lang.Boolean::FALSE : o;
  }-*/;

  @UnsafeNativeLong
  public static native Long boxLong(JavaScriptObject o)
  /*-{
  	if (typeof o === 'number') {
  		o = ~~o + '';
  	}
  	if (typeof o === 'string') {
  		return @java.lang.Long::new(Ljava/lang/String;)(o).@java.lang.Long::longValue()();
  	}
  	if (o.l == undefined) {
  		// Can fail if o is not a number, but it will never fail if o is a primitive long
  		return o.@java.lang.Number::longValue()();
  	}
  	return o;
  }-*/;

  @UnsafeNativeLong
  public static native String boxArray(JavaScriptObject o, String joiner)
  /*-{
    if (!o) {
      return "";
    }
    if (!joiner) {
      joiner = "  ";
      // If using default joiner, let's eat all double spaces in any input strings.
      // Though the string will lose the ability to have double spacing, it won't have strange breakage.
      // If you need to support double spaces, be sure to supply your own joiner string,
      // if using generated code, a @JsProperty String joiner(); method should suffice.
      for (var i = o.length; i-->0;) {
        if (o[i]&&o[i].replace) {
          o[i] = o[i].replace(/([ ][ ]+)/ , " ");
        }
      }
    }
    if (o.join) {
      return o.join(joiner);
    }
    return o.toString();
  }-*/;

  public static native byte unboxByte(JavaScriptObject o)
  /*-{
  	if (o == undefined) {
  		return 0;
  	}
  	if (typeof o === 'string') {
  		o = parseInt(o);
  	}
  	return typeof o === 'number' ? ~~o : o.@java.lang.Number::byteValue()();
  }-*/;

  public static native short unboxShort(JavaScriptObject o)
  /*-{
  	if (o == undefined) {
  		return 0;
  	}
  	if (typeof o === 'string') {
  		o = parseInt(o);
  	}
  	return typeof o === 'number' ? ~~o
  			: o.@java.lang.Number::shortValue()();
  }-*/;

  public static native int unboxInteger(JavaScriptObject o)
  /*-{
  	if (o == undefined) {
  		return 0;
  	}
  	if (typeof o === 'string') {
  		o = parseInt(o);
  	}
  	return typeof o === 'number' ? ~~o : o.@java.lang.Number::intValue()();
  }-*/;

  public static native float unboxFloat(JavaScriptObject o)
  /*-{
  	if (o == undefined) {
  		return 0;
  	}
  	if (typeof o === 'string') {
  		o = parseFloat(o);
  	}
  	return typeof o === 'number' ? o : o.@java.lang.Number::floatValue()();
  }-*/;

  public static native double unboxDouble(JavaScriptObject o)
  /*-{
  	if (o == undefined) {
  		return 0;
  	}
  	if (typeof o === 'string') {
  		o = parseFloat(o);
  	}
  	return typeof o === 'number' ? o : o.@java.lang.Number::doubleValue()();
  }-*/;

  public static native char unboxCharacter(JavaScriptObject o)
  /*-{
  	if (o == undefined) {
  		return 0;
  	}
  	if (typeof o === 'string') {
  		o = o && o.charAt(0) || 0;
  	}
  	return typeof o === 'number' ? o
  			: o.@java.lang.Character::charValue()();
  }-*/;

  public static native boolean unboxBoolean(JavaScriptObject o)
  /*-{
  	if (o == undefined) {
  		return false;
  	}
  	return typeof o === 'boolean' ? o
  			: typeof o === 'string' ? o === 'true'
  					: o.@java.lang.Boolean::booleanValue()();
  }-*/;

  @UnsafeNativeLong
  public static native long unboxLong(JavaScriptObject o)
  /*-{
  	if (o == undefined) {
  		o = '0';
  	}
  	if (typeof o === 'number') {
  		o = o.toString();
  	}
  	if (typeof o === 'string') {
  		return @java.lang.Long::new(Ljava/lang/String;)(o).@java.lang.Long::longValue()();
  	}
  	if (o.l == undefined) {
  		return o.@java.lang.Number::longValue()();
  	}
  	return o;
  }-*/;

  /**
   * Constructs a "fake number" that works in both GWT and javascript.
   *
   * This number has the format java expects: {l:0, m:0, h:0}, but also
   * overrides the .valueOf() function to act like a Long.doubleValue() in
   * javascript.
   *
   * Operations in javascript will lose precision due to the use of 52 bit
   * numbers, so if you need full long precision, use only methods originating
   * from java that use the object format.
   *
   * @param o
   *          -> A number in any format that can be expected from javascript
   *          (number, string, Long or long)
   * @return a hybrid long with shims to act like a javascript number.
   */
  @UnsafeNativeLong
  public static native long unboxLongNative(JavaScriptObject o)
  /*-{
  	var javaLong = @xapi.components.impl.JsSupport::unboxLong(Lcom/google/gwt/core/client/JavaScriptObject;)(o);
  	var tricky = function() {
  		return @java.lang.Long::new(J)(javaLong).@java.lang.Long::doubleValue()();
  	}
  	tricky.l = javaLong.l;
  	tricky.m = javaLong.m;
  	tricky.h = javaLong.h;
  	tricky.valueOf = function() {
  		return @java.lang.Long::new(J)(javaLong).@java.lang.Long::doubleValue()();
  	}
  	return tricky;
  }-*/;

  public static native JsArrayOfString unboxArrayOfString(JavaScriptObject o,
      String joiner)
  /*-{
    if (!o) {
      return [];
    }
    if (!joiner) {
      joiner = "  ";
    }
    return o.toString().split(joiner);
  }-*/;

  public static native JsArrayOfInt unboxArrayOfInt(JavaScriptObject o,
      String joiner)
  /*-{
    if (!o) {
      return [];
    }
    if (!joiner) {
      joiner = "  ";
    }
    var arr = o.toString().split(joiner);
    for (var i = arr.length; i-->0;) {
      arr[i] = parseInt(arr[i]);
    }
    return arr;
  }-*/;

  public static native JsArrayOfNumber unboxArrayOfNumber(JavaScriptObject o,
      String joiner)
  /*-{
    if (!o) {
      return [];
    }
    if (!joiner) {
      joiner = "  ";
    }
    var arr = o.toString().split(joiner);
    for (var i = arr.length; i-->0;) {
      arr[i] = parseFloat(arr[i]);
    }
    return arr;
  }-*/;

  public static native JsArrayOfBoolean unboxArrayOfBoolean(JavaScriptObject o,
      String joiner)
  /*-{
    if (!o) {
      return [];
    }
    if (!joiner) {
      joiner = "  ";
    }
    var arr = o.toString().split(joiner);
    for (var i = arr.length; i-->0;) {
      arr[i] = arr[i] === 'true';
    }
    return arr;
  }-*/;

  public static native boolean not(Object o)
  /*-{
    return !o;
  }-*/;

  public static native boolean is(Object o)
  /*-{
    return !!o;
  }-*/;

  public static native boolean jsEquals(Object o1, Object o2)
  /*-{
    return o1 === o2;
  }-*/;

  public static native boolean jsNotEquals(Object o1, Object o2)
  /*-{
    return o1 !== o2;
  }-*/;

  public static String newId() {

    Document doc = doc();
    String id = randomId();
    // Gets a new, unused id.
    // This is preferable to using a static int,
    // as there may be more than one compiled app running in the page.
    while (doc.getElementById(id) != null) {
      id = randomId();
    }
    return id;
  }

  private static native String randomId()
  /*-{
    // We want a random number that starts with a letter.
    // We will do this using base 36 toString method,
    // and we will use a number between 0.3 and 0.99999
    var i = 0.3 + 0.7 * Math.random();
    // Ditch the leading "0."
    return i.toString(36).substr(2);
  }-*/;

  public static <E extends Element> E newElement(String tagName) {
    return doc().createElement(tagName);
  }

  public static DivElement newDivWithHtml(String html) {
    DivElement div = newElement("div");
    div.setInnerHTML(html);
    return div;
  }

  public static <J extends JavaScriptObject> J create() {

    return JavaScriptObject.createObject().cast();
  }

  public static native Element setAttr(Element group, String string)
  /*-{
    group.setAttribute(string, "");
    return group;
  }-*/;

  public static void hideIfEmpty(Element e) {

    if (e.getInnerText().isEmpty()) {
      e.getStyle().setDisplay("none");
    } else {
      e.getStyle().removeProperty("display");
    }
  }

  public static native boolean isFunction(Object a)
  /*-{
    return typeof a === 'function';
  }-*/;

  public static native <A> A invoke(Object func, Object args)
  /*-{
    return func.call(args);
  }-*/;

  public static native boolean exists(Object object, String name)
  /*-{
    return name in object;
   }-*/;

  public static native boolean attributeExists(Object object, String name)
  /*-{
    return object.getAttribute && object.getAttribute(name) != null;
   }-*/;

  public static native boolean attributeEquals(Object object, String name, String value)
  /*-{
     $wnd.console.log(object, name, value);
    return object.getAttribute && object.getAttribute(name) === value;
   }-*/;

  public static native void setBoolean(Object object, String key, boolean value)
  /*-{
    object[key]=value;
  }-*/;

  public static native void setByte(Object object, String key, byte value)
  /*-{
    object[key]=value;
  }-*/;

  public static native void setShort(Object object, String key, short value)
  /*-{
    object[key]=value;
  }-*/;

  public static native void setChar(Object object, String key, char value)
  /*-{
    object[key]=value;
  }-*/;

  public static native void setInt(Object object, String key, int value)
  /*-{
    object[key]=value;
  }-*/;

  @UnsafeNativeLong
  public static native void setLong(Object object, String key, long value)
  /*-{
    object[key]=value;
  }-*/;

  public static native void setFloat(Object object, String key, float value)
  /*-{
    object[key]=value;
  }-*/;

  public static native void setDouble(Object object, String key, double value)
  /*-{
    object[key]=value;
  }-*/;

  public static native void setObject(Object object, String key, Object value)
  /*-{
    object[key]=value;
  }-*/;

  public static native Object getObject(Object object, String key)
  /*-{
    return object[key];
  }-*/;

  public static native String getString(Object object, String key)
  /*-{
    return object[key]||null;//coerce undefined
  }-*/;

  public static native int getInt(Object object, String key)
  /*-{
    var i = object[key]||null;
    if (i) {
      if (typeof i === 'number') {
        return ~~i;
      }
      return @xapi.components.impl.JsSupport::unboxInteger(Lcom/google/gwt/core/client/JavaScriptObject;)(i);
    }
    return 0;
  }-*/;

  public static native int getDouble(Object object, String key)
  /*-{
    var i = object[key]||null;
    if (i) {
      if (typeof i === 'number') {
        return i;
      }
      return @xapi.components.impl.JsSupport::unboxDouble(Lcom/google/gwt/core/client/JavaScriptObject;)(i);
    }
    return 0;
  }-*/;

  @UnsafeNativeLong
  public static native long getLong(Object object, String key)
  /*-{
    var i = object[key]||null;
    if (i) {
      if (typeof i === 'object' && typeof i.l === 'number') {
        return i;
      }
      return @xapi.components.impl.JsSupport::unboxLong(Lcom/google/gwt/core/client/JavaScriptObject;)(i);
    }
    return {l:0, m:0, h:0};
  }-*/;

  public static native boolean getBoolean(Object object, String key)
  /*-{
    var i = object[key]||null;
    if (i) {
      if (typeof i === 'boolean') {
        return i;
      }
      return @xapi.components.impl.JsSupport::unboxBoolean(Lcom/google/gwt/core/client/JavaScriptObject;)(i);
    }
    return false;
  }-*/;

  public static native Element createShadowRoot(Element e)
  /*-{
    return e.createShadowRoot();
  }-*/;

  public static native JsArrayOfString split(String value, String on)
  /*-{
    return value.split(on);
  }-*/;

  public static void addClassName(Element e, String className) {

    String was = e.getClassName();
    if (was.isEmpty()) {
      e.setClassName(className);
    } else if (!((" " + was + " ").contains(" " + className + " "))) {
      e.setClassName(was + " " + className);
    }
  }

  public static String interleave(JsArrayOfString array, String key) {

    if (array.isEmpty())
      return "";
    return key + " " + array.join(" " + key) + " ";
  }

  public static void removeFromParent(Element item) {

    Element parent = item.getParentElement();
    if (parent != null) {
      parent.removeChild(item);
    }
  }

  public static native Element findInShadowRoot(Element element, String id)
  /*-{
    return element.shadowRoot.getElementById(id);
  }-*/;

  public static native JavaScriptObject copy(Object o)
  /*-{
    return Object.create(o);
  }-*/;

  public static <I, O> In1Out1<I, O> nativeFactory(In1Out1<I, O> factory, String name) {
    return factory.lazy(new NativeFactory<>(name));
  }
}
