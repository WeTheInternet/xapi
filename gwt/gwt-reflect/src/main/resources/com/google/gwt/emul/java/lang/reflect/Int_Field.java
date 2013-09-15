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
 * A field representing a int short.
 * 
 * @author "james@wetheinter.net"
 * 
 */
public final class Int_Field extends Field {

  private boolean expectChar;
  
  public Int_Field(Class<?> declaringClass, String name, int modifiers, JavaScriptObject accessor) {
    super(int.class, declaringClass, name, modifiers, accessor);
  }

  protected final Object nativeGet(Object obj) {
    return new Integer(primitiveGet(obj));
  }

  protected final void nativeSet(Object obj, Object value) {
    if (expectChar && value instanceof Character) {
      expectChar = false;
      primitiveSet(obj, ((Character) value).charValue());
      return;
    }
    primitiveSet(obj, ((Number) value).intValue());
  }
  
  protected boolean isNotAssignable (Class<?> c) {
    if (c == Integer.class) {
      return (expectChar = false);
    }
    if (Number.class.isAssignableFrom(c)) {
      expectChar = false;
      return c != Byte.class && c != Short.class;
    }
    expectChar = c == Character.class;
    return !expectChar;
  }

  protected final native int primitiveGet(Object obj)
  /*-{
    return this.@java.lang.reflect.Field::accessor.getter(obj);
   }-*/;

  protected final native void primitiveSet(Object obj, int value)
  /*-{
    this.@java.lang.reflect.Field::accessor.setter(obj, value);
   }-*/;

  protected boolean nullNotAllowed() {
    return true;
  }

  public final int getInt(Object obj) throws IllegalArgumentException,
      IllegalAccessException {
    maybeThrowNull(obj);
    return primitiveGet(obj);
  }
  
  public final long getLong(Object obj) throws IllegalArgumentException,
  IllegalAccessException {
    maybeThrowNull(obj);
    return primitiveGet(obj);
  }
  
  public final float getFloat(Object obj) throws IllegalArgumentException,
  IllegalAccessException {
    maybeThrowNull(obj);
    return primitiveGet(obj);
  }
  
  public final double getDouble(Object obj) throws IllegalArgumentException,
  IllegalAccessException {
    maybeThrowNull(obj);
    return primitiveGet(obj);
  }

  public final void setInt(Object obj, int i)
      throws IllegalArgumentException, IllegalAccessException {
    maybeThrowNull(obj);
    primitiveSet(obj, i);
  }

}
