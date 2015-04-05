package com.google.gwt.reflect.shared;

import com.google.gwt.core.client.JavaScriptObject;

import java.lang.reflect.Method;
import java.security.ProtectionDomain;

/**
 * This is the abstract base class used to encapsulate runtime reflection data
 * for enhanced classes.
 * <p>
 * This class includes some specific, common containers, like enclosing classes
 * or methods, any inner classes or interfaces, the protection domain, and a
 * {@link #newInstance()} method.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 * @param <T>
 */
public abstract class ClassMap<T> {

  protected static native void remember(int constId, ClassMap<?> cls)
  /*-{
  	$wnd.GwtReflect.$[constId] = cls;
  }-*/;

  public JavaScriptObject ifaces = JavaScriptObject.createArray();
  public JavaScriptObject classes = JavaScriptObject.createArray();

  private Method enclosingMethod;
  private Class<?> enclosingClass;

  public native void addClass(Class<?> cls, JavaScriptObject into)
  /*-{
  	into[into.length] = cls;
  }-*/;

  public final Class<?>[] getDeclaredClasses() {
    return ReflectUtil.getRawClasses(classes);
  }

  public Class<?> getEnclosingClass() {
    return enclosingClass;
  }

  public Method getEnclosingMethod() {
    return enclosingMethod;
  }

  public final Class<?>[] getInterfaces() {
    return ReflectUtil.getRawClasses(ifaces);
  }

  public ProtectionDomain getProtectionDomain() {
    return null;
  }

  public T newInstance() {
    throw new UnsupportedOperationException();
  }

}
