package com.google.gwt.reflect.client;

import java.lang.reflect.Method;
import java.security.ProtectionDomain;

import com.google.gwt.core.client.JavaScriptObject;


public abstract class ClassMap <T> {

  public JavaScriptObject ifaces = JavaScriptObject.createArray();
  public JavaScriptObject classes = JavaScriptObject.createArray();
  private Method enclosingMethod;
  private Class<?> enclosingClass;

  public final Class<?>[] getInterfaces() {
    return MemberMap.getPublicMembers(ifaces, new Class[0]);
  }

  public final Class<?>[] getDeclaredClasses() {
    return MemberMap.getRawClasses(classes);
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
    @com.google.gwt.reflect.client.ConstPool::CONSTS.$[constId] = cls;
   }-*/;
  
  public Method getEnclosingMethod() {
    return enclosingMethod;
  }
  
  public Class<?> getEnclosingClass() {
    return enclosingClass;
  }

  private static void throwIllegalAccess() throws IllegalAccessException {
    throw new IllegalAccessException();
  }
  
}
