package java.lang.reflect;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * A field representing a primitive short.
 * 
 * @author "james@wetheinter.net"
 * 
 */
public final class Short_Field extends Field {

  public Short_Field(Class<?> declaringClass, String name, int modifiers, JavaScriptObject accessor) {
    super(short.class, declaringClass, name, modifiers, accessor);
  }

  protected final Object nativeGet(Object obj) {
    return new Short(primitiveGet(obj));
  }

  protected final void nativeSet(Object obj, Object value) {
    primitiveSet(obj, ((Number) value).shortValue());
  }
  
  protected boolean isNotAssignable (Class<?> c) {
    if (c == Short.class)
      return false;
    return c != Byte.class;
  }

  protected final native short primitiveGet(Object obj)
  /*-{
    return this.@java.lang.reflect.Field::accessor.getter(obj);
   }-*/;

  protected final native void primitiveSet(Object obj, short value)
  /*-{
    this.@java.lang.reflect.Field::accessor.setter(obj, value);
   }-*/;

  protected boolean nullNotAllowed() {
    return true;
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

  public final void setShort(Object obj, short s)
      throws IllegalArgumentException, IllegalAccessException {
    maybeThrowNull(obj);
    primitiveSet(obj, s);
  }

}
