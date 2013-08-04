package com.google.gwt.reflect.client;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.google.gwt.core.client.JavaScriptObject;

public class JsMemberPool <T> extends JavaScriptObject implements MemberPool <T>{

  protected JsMemberPool() {}

  public static native <T> JsMemberPool<T> createMemberPool(Class<T> type)
  /*-{
    return {
      $:type,
      _:{},// array classes
      a:{},// annotations
      c:{},// constructors
      f:{},// fields
      i:{},// interfaces
      m:{},// methods
      t:{},// inner types
    };
  }-*/;

  @Override
  public final native <A extends Annotation> A getAnnotation(Class<A> annoCls)
  /*-{
    return this.a[annoCls.@java.lang.Class::getName()()];
  }-*/;

  @Override
  public final native Annotation[] getAnnotations()
  /*-{
    var array = @com.google.gwt.reflect.client.JsMemberPool::annoArray()();
    for (var i in this.a) {
      array.push(this.a[i]());
    }
    return array;
  }-*/;

  @Override
  public final Annotation[] getDeclaredAnnotations() {
    // we currently don't differentiate between declared and public annotations
    return getAnnotations();
  }

  @Override
  public final Constructor<T> getConstructor(Class<?> ... params) throws NoSuchMethodException {
    String key = JsMemberPool.getSignature(params);

    return null;
  }

  @Override
  public final Constructor<T> getDeclaredConstructor(Class<?> ... params) throws NoSuchMethodException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public final Constructor<T>[] getConstructors() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public final Constructor<T>[] getDeclaredConstructors() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public final Field getField(String name) throws NoSuchFieldException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public final Field getDeclaredField(String name) throws NoSuchFieldException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public final Field[] getFields() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public final Field[] getDeclaredFields() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public final Method getDeclaredMethod(String name, Class<?> ... params) throws NoSuchMethodException {
    String id = name + getSignature(params);
    Method method = findMethod(this, id);
    if (method != null)
      return method;
    throw new NoSuchMethodException("Could not find declared method "+name+
        "("+GwtReflect.joinClasses(",",  params)+") in "+getType());
  }

  @Override
  public final Method getMethod(String name, Class<?> ... params) throws NoSuchMethodException {
    String id = name + getSignature(params);
    JsMemberPool<? super T> pool = this;
    while (pool != null) {
      Method method = findMethod(pool, id);
      if (method == null)
        pool = pool.getSuperclass();
      else if (method.getModifiers() == Modifier.PUBLIC){
        return method;
      }
      else 
        break; // super classes can't match if the subclass has a lower privacy method
    }
    throw new NoSuchMethodException("Could not find public method "+name+
        "("+GwtReflect.joinClasses(",",  params)+") in "+getType());
  }

  private static native Method findMethod(JsMemberPool<?> pool, String id)
  /*-{
    return pool.m.hasOwnProperty(id) && pool.m[id];
  }-*/;

  private static native void fillMethods(JsMemberPool<?> pool, Method[] array)
  /*-{
    for (var i in pool.m)
      if (pool.m.hasOwnProperty(i))
        array[array.length] = pool.m[i];
  }-*/;


  private static native boolean isUnique(JavaScriptObject set, String key)
  /*-{
    return set[key] ? false : (set[key] = true);
  }-*/;

  @Override
  public final Method[] getMethods() {
    Method[] methods = methodArray();
    JsMemberPool<? super T> pool = this;
    JavaScriptObject set = JavaScriptObject.createObject();
    while (pool != null) {
      for (Method declared : pool.getDeclaredMethods()) {
        if (declared.getModifiers() == Modifier.PUBLIC &&
            isUnique(set, declared.getName()+getSignature(declared.getParameterTypes()))) {
          // javascript actually likes this; preallocating arrays doesn't save time
          methods[methods.length] = declared;
        }
      }
      pool = pool.getSuperclass();
    }
    return methods;
  }

  @Override
  public final Method[] getDeclaredMethods() {
    Method[] methods = methodArray();
    fillMethods(this, methods);
    return methods;
  }

  @Override
  public final JsMemberPool<? super T> getSuperclass() {
    Class<? super T> superClass = getType().getSuperclass();
    assert superClass != getType();
    return superClass == null ? null : ConstPool.getMembers(superClass);
  }

  @Override
  public final MemberPool<? super T>[] getInterfaces() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public final MemberPool<?>[] getClasses() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public final native Class<T> getType()
  /*-{
    return this.$;
  }-*/;

  static native int getSeedId(Class<?> cls)
  /*-{
    return cls.@java.lang.Class::seedId;
  }-*/;
  
  static void addMethod(Class<?> cls, Method m) {
    JsMemberPool<?> pool = ConstPool.getMembers(cls);
    pool.addMethod(m);
  }

  public final void addMethod(Method m) {
    String name = m.getName()+getSignature(m.getParameterTypes());
    addMethod(name, m);
  }
  
  private final native void addMethod(String key, Method m)
  /*-{
    this.m[key] = m;
  }-*/;

  static String getSignature(Class<?> ... signature) {
    StringBuilder key = new StringBuilder();
    for (int i = 0; i < signature.length; i++) {
      key.append('_');
      key.append(ConstPool.constId(signature[i]));
    }
    return key.toString();
  }

  private static Annotation[] annoArray() {return new Annotation[0];}
  private static Class<?>[] classArray() {return new Class<?>[0];}
  private static Method[] methodArray() {return new Method[0];}
  private static Field[] fieldArray() {return new Field[0];}

}
