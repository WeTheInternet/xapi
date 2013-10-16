package java.lang.reflect;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;

/**
 * A field representing a long short.
 * 
 * @author "james@wetheinter.net"
 * 
 */
public final class Long_Field extends Field {

  private boolean expectChar;
  
  public Long_Field(Class<?> declaringClass, String name, int modifiers, JavaScriptObject accessor) {
    super(long.class, declaringClass, name, modifiers, accessor);
  }

  protected final Object nativeGet(Object obj) {
    return new Long(primitiveGet(obj));
  }

  protected final void nativeSet(Object obj, Object value) {
    if (expectChar) {
      expectChar = false;
      primitiveSet(obj, ((Character) value).charValue());
    } else {
      primitiveSet(obj, ((Number) value).longValue());
    }
  }
  
  protected boolean isNotAssignable (Class<?> c) {
    if (c == Long.class)
      return (expectChar = false);
    if (Number.class.isAssignableFrom(c)) {
      expectChar = false;
      return c == Float.class || c == Double.class;
    }
    expectChar = c == Character.class;
    return !expectChar;
  }

  @UnsafeNativeLong
  protected final native long primitiveGet(Object obj)
  /*-{
    return this.@java.lang.reflect.Field::accessor.getter(obj);
   }-*/;

  @UnsafeNativeLong
  protected final native void primitiveSet(Object obj, long value)
  /*-{
    this.@java.lang.reflect.Field::accessor.setter(obj, value);
   }-*/;

  protected boolean nullNotAllowed() {
    return true;
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

  public final void setLong(Object obj, long l)
      throws IllegalArgumentException, IllegalAccessException {
    maybeThrowNull(obj);
    primitiveSet(obj, l);
  }

}
