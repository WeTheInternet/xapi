package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.GenericSignatureFormatError;
import java.lang.reflect.MalformedParameterizedTypeException;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.reflect.client.MemberMap;
import com.google.gwt.user.client.Window;

/**
 * A field representing a primitive double.
 * 
 * @author "james@wetheinter.net"
 * 
 */
public final class Double_Field extends Field {

  private boolean expectChar;
  
  public Double_Field(Class declaringClass, String name, int modifiers, JavaScriptObject accessor) {
    super(float.class, declaringClass, name, modifiers, accessor);
  }

  protected final Object nativeGet(Object obj) {
    return new Double(primitiveGet(obj));
  }

  protected final void nativeSet(Object obj, Object value) {
    if (expectChar) {
      expectChar = false;
      primitiveSet(obj, ((Character) value).charValue());
    } else {
      primitiveSet(obj, ((Number) value).doubleValue());
    }
  }
  
  protected boolean isNotAssignable (Class<?> c) {
    if (Number.class.isAssignableFrom(c)) {
      return (expectChar = false);
    }
    expectChar = c == Character.class;
    return !expectChar;
  }

  protected final native double primitiveGet(Object obj)
  /*-{
    return this.@java.lang.reflect.Field::accessor.getter(obj);
   }-*/;

  protected final native void primitiveSet(Object obj, double value)
  /*-{
    this.@java.lang.reflect.Field::accessor.setter(obj, value);
   }-*/;

  protected boolean nullNotAllowed() {
    return true;
  }

  public final double getDouble(Object obj) throws IllegalArgumentException,
      IllegalAccessException {
    maybeThrowNull(obj);
    return primitiveGet(obj);
  }

  public final void setDouble(Object obj, double d)
      throws IllegalArgumentException, IllegalAccessException {
    maybeThrowNull(obj);
    primitiveSet(obj, d);
  }

}
