package java.lang.reflect;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * A field representing a float short.
 * 
 * @author "james@wetheinter.net"
 * 
 */
public final class Float_Field extends Field {

  private boolean expectChar;
  
  public Float_Field(Class<?> declaringClass, String name, int modifiers, JavaScriptObject accessor) {
    super(float.class, declaringClass, name, modifiers, accessor);
  }

  protected final Object nativeGet(Object obj) {
    return new Float(primitiveGet(obj));
  }

  protected final void nativeSet(Object obj, Object value) {
    if (expectChar) {
      expectChar = false;
      primitiveSet(obj, ((Character) value).charValue());
    } else {
      primitiveSet(obj, ((Number) value).floatValue());
    }
  }
  
  protected boolean isNotAssignable (Class<?> c) {
    if (c == Float.class)
      return (expectChar = false);
    if (Number.class.isAssignableFrom(c)) {
      expectChar = false;
      return c == Double.class;
    }
    expectChar = c == Character.class;
    return !expectChar;
  }

  protected final native float primitiveGet(Object obj)
  /*-{
    return this.@java.lang.reflect.Field::accessor.getter(obj);
   }-*/;

  protected final native void primitiveSet(Object obj, float value)
  /*-{
    this.@java.lang.reflect.Field::accessor.setter(obj, value);
   }-*/;

  protected boolean nullNotAllowed() {
    return true;
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

  public final void setFloat(Object obj, float f)
      throws IllegalArgumentException, IllegalAccessException {
    maybeThrowNull(obj);
    primitiveSet(obj, f);
  }

}
