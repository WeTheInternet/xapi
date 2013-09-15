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
 * A field representing a byte short.
 * 
 * @author "james@wetheinter.net"
 * 
 */
public final class Byte_Field extends Field {

  public Byte_Field(Class declaringClass, String name, int modifiers, JavaScriptObject accessor) {
    super(byte.class, declaringClass, name, modifiers, accessor);
  }

  protected final Object nativeGet(Object obj) {
    return new Byte(primitiveGet(obj));
  }

  protected final void nativeSet(Object obj, Object value) {
    primitiveSet(obj, (Byte) value);
  }
  
  protected boolean isNotAssignable (Class<?> c) {
    return c != Byte.class;
  }

  protected final native byte primitiveGet(Object obj)
  /*-{
    return this.@java.lang.reflect.Field::accessor.getter(obj);
   }-*/;

  protected final native void primitiveSet(Object obj, byte value)
  /*-{
    this.@java.lang.reflect.Field::accessor.setter(obj, value);
   }-*/;

  protected boolean nullNotAllowed() {
    return true;
  }

  public final byte getByte(Object obj) throws IllegalArgumentException,
      IllegalAccessException {
    maybeThrowNull(obj);
    return primitiveGet(obj);
  }
  
  public final short getShort(Object obj) throws IllegalArgumentException,
  IllegalAccessException {
    maybeThrowNull(obj);
    return primitiveGet(obj);
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
  

  public final void setByte(Object obj, byte b)
      throws IllegalArgumentException, IllegalAccessException {
    maybeThrowNull(obj);
    primitiveSet(obj, b);
  }

}
