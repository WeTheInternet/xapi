package com.google.gwt.reflect.shared;

import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import com.google.gwt.core.client.JavaScriptObject;

public abstract class ClassMap <T> {

  public JavaScriptObject ifaces = JavaScriptObject.createArray();
  public JavaScriptObject classes = JavaScriptObject.createArray();
  private Method enclosingMethod;
  private Class<?> enclosingClass;

  public final Class<?>[] getInterfaces() {
    return ReflectUtil.getRawClasses(ifaces);
  }

  public final Class<?>[] getDeclaredClasses() {
    return ReflectUtil.getRawClasses(classes);
  }

  public native void addClass(Class<?> cls, JavaScriptObject into)
  /*-{
    into[into.length] = cls;
  }-*/;

  public abstract T newInstance();
  
  public ProtectionDomain getProtectionDomain() {
    return null;
  }

  protected static native void remember(int constId, ClassMap<?> cls)
  /*-{
    $wnd.Reflect.$[constId] = cls;
   }-*/;
  
  public Method getEnclosingMethod() {
    return enclosingMethod;
  }
  
  public Class<?> getEnclosingClass() {
    return enclosingClass;
  }

}
