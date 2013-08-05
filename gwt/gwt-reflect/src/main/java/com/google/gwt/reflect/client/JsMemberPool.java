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

  private static native void fillConstructors(JsMemberPool<?> pool, Constructor[] array)
  /*-{
    for (var i in pool.c)
      if (pool.c.hasOwnProperty(i))
        array[array.length] = pool.c[i];
  }-*/;

  private static native void fillFields(JsMemberPool<?> pool, Field[] array)
  /*-{
    for (var i in pool.f)
      if (pool.f.hasOwnProperty(i))
        array[array.length] = pool.f[i];
  }-*/;

  private static native void fillMethods(JsMemberPool<?> pool, Method[] array)
  /*-{
    for (var i in pool.m)
      if (pool.m.hasOwnProperty(i))
        array[array.length] = pool.m[i];
  }-*/;

  private static native <T> Constructor<T> findConstructor(JsMemberPool<T> pool, String id)
  /*-{
    return pool.c.hasOwnProperty(id) && pool.c[id];
  }-*/;

  private static native Field findField(JsMemberPool<?> pool, String id)
  /*-{
    return pool.f.hasOwnProperty(id) && pool.f[id];
  }-*/;

  private static native Method findMethod(JsMemberPool<?> pool, String id)
  /*-{
    return pool.m.hasOwnProperty(id) && pool.m[id];
  }-*/;

  private static native boolean isUnique(JavaScriptObject set, String key)
  /*-{
    return set[key] ? false : (set[key] = true);
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
  @SuppressWarnings("unchecked")
  private static <T> Constructor<T>[] constructorArray() {return new Constructor[0];}
  private static Field[] fieldArray() {return new Field[0];}
  private static Method[] methodArray() {return new Method[0];}
  
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
      if (this.a.hasOwnProperty(i))
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
  public final Constructor getConstructor(Class<?> ... params) throws NoSuchMethodException {
    String id = getSignature(params);
    JsMemberPool<? super T> pool = this;
    while (pool != null) {
      Constructor method = findConstructor(pool, id);
      if (method == null)
        pool = pool.getSuperclass();
      else if (Modifier.isPublic(method.getModifiers())){
        return method;
      }
      else 
        break; // super classes can't match if the subclass has a lower privacy method
    }
    throw new NoSuchMethodException("Could not find public constructor "+getType().getSimpleName()+
        "("+GwtReflect.joinClasses(",",  params)+")");
  }

  @Override
  public final Constructor<T> getDeclaredConstructor(Class<?> ... params) throws NoSuchMethodException {
    String id = getSignature(params);
    Constructor<T> ctor = findConstructor(this, id);
    if (ctor != null)
      return ctor;
    throw new NoSuchMethodException("Could not find declared constructor "+getType().getSimpleName()+
        "("+GwtReflect.joinClasses(",",  params)+") in "+getType());
  }

  @Override
  public final Constructor[] getConstructors() {
    Constructor[] ctors = constructorArray();
    JsMemberPool<? super T> pool = this;
    JavaScriptObject set = JavaScriptObject.createObject();
    while (pool != null) {
      for (Constructor declared : pool.getDeclaredConstructors()) {
        if (Modifier.isPublic(declared.getModifiers()) &&
            isUnique(set, getSignature(declared.getParameterTypes()))) {
          // javascript actually likes this; preallocating arrays doesn't save time
          ctors[ctors.length] = declared;
        }
      }
      pool = pool.getSuperclass();
    }
    return ctors;
  }

  @Override
  public final Constructor<T>[] getDeclaredConstructors() {
    Constructor<T>[] ctors = constructorArray();
    fillConstructors(this, ctors);
    return ctors;
  }

  @Override
  public final Field getField(String name) throws NoSuchFieldException {
    JsMemberPool<? super T> pool = this;
    while (pool != null) {
      Field field = findField(pool, name);
      if (field == null)
        pool = pool.getSuperclass();
      else if (Modifier.isPublic(field.getModifiers())){
        return field;
      }
      else 
        break; // super classes can't match if the subclass has a lower privacy method
    }
    throw new NoSuchFieldException("Could not find public field "+name+ " in "+getType());
  }

  @Override
  public final Field getDeclaredField(String name) throws NoSuchFieldException {
    Field field = findField(this, name);
    if (field != null)
      return field;
    throw new NoSuchFieldException("Could not find declared field "+name+" in "+getType());
  }

  @Override
  public final Field[] getFields() {
    Field[] fields = fieldArray();
    JsMemberPool<? super T> pool = this;
    JavaScriptObject set = JavaScriptObject.createObject();
    while (pool != null) {
      for (Field declared : pool.getDeclaredFields()) {
        if (Modifier.isPublic(declared.getModifiers()) &&
            isUnique(set, declared.getName())) {
          fields[fields.length] = declared;
        }
      }
      pool = pool.getSuperclass();
    }
    return fields;
  }

  @Override
  public final Field[] getDeclaredFields() {
    Field[] fields = fieldArray();
    fillFields(this, fields);
    return fields;
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
      else if (Modifier.isPublic(method.getModifiers())){
        return method;
      }
      else 
        break; // super classes can't match if the subclass has a lower privacy method
    }
    throw new NoSuchMethodException("Could not find public method "+name+
        "("+GwtReflect.joinClasses(",",  params)+") in "+getType());
  }
  
  @Override
  public final Method[] getMethods() {
    Method[] methods = methodArray();
    JsMemberPool<? super T> pool = this;
    JavaScriptObject set = JavaScriptObject.createObject();
    while (pool != null) {
      for (Method declared : pool.getDeclaredMethods()) {
        if (Modifier.isPublic(declared.getModifiers()) &&
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
  public final Class<?>[] getInterfaces() {
    return getType().getInterfaces();
  }

  @Override
  public final Class<?>[] getClasses() {
    return getType().getClasses();
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
  
  @SuppressWarnings("rawtypes")
  static void addConstructor(Class<?> cls, Constructor c) {
    JsMemberPool<?> pool = ConstPool.getMembers(cls);
    pool.addConstructor(c);
  }
  
  @SuppressWarnings("rawtypes")
  public final void addConstructor(Constructor c) {
    addConstructor(getSignature(c.getParameterTypes()), c);
  }
  
  @SuppressWarnings("rawtypes")
  private final native void addConstructor(String key, Constructor c)
  /*-{
    this.c[key] = c;
  }-*/;
  
  static void addField(Class<?> cls, Field f) {
    JsMemberPool<?> pool = ConstPool.getMembers(cls);
    pool.addField(f);
  }

  public final void addField(Field f) {
    addField(f.getName(), f);
  }
  
  private final native void addField(String key, Field f)
  /*-{
    this.f[key] = f;
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

}
