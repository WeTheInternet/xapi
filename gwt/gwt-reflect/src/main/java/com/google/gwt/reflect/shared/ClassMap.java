package com.google.gwt.reflect.shared;

import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import com.google.gwt.core.client.JavaScriptObject;

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

  public abstract T newInstance();

}
