package com.google.gwt.reflect.shared;

import com.google.gwt.core.client.JavaScriptObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class JsMemberPool <T> extends JavaScriptObject implements MemberPool <T>{

  public static native int constId(Class<?> cls)
  /*-{
     cls.@java.lang.Class::getName()(); // ensures constId is initialized
     return cls.@java.lang.Class::constId;
   }-*/;

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

  public static <T> JsMemberPool<T> getMembers(final Class<T> cls) {
    final int constId = constId(cls);
    JsMemberPool<T> members = findMembers(constId);
    if (members == null) {
      members = createMemberPool(cls);
      setMembers(constId, members);
    }
    return members;
  }

  @SuppressWarnings("rawtypes")
  static void addConstructor(final Class<?> cls, final Constructor c) {
    final JsMemberPool<?> pool = getMembers(cls);
    pool.addConstructor(c);
  }

  static void addField(final Class<?> cls, final Field f) {
    final JsMemberPool<?> pool = getMembers(cls);
    pool.addField(f);
  }

  static void addMethod(final Class<?> cls, final Method m) {
    final JsMemberPool<?> pool = getMembers(cls);
    pool.addMethod(m);
  }

  static void addAnnotation(final Class<?> cls, final Annotation a, final boolean declared) {
    final JsMemberPool<?> pool = getMembers(cls);
    pool.addAnnotation(a, declared);
  }

  static native int getSeedId(Class<?> cls)
  /*-{
    return cls.@java.lang.Class::typeId;
  }-*/;

  static String getSignature(final Class<?> ... signature) {
    final StringBuilder key = new StringBuilder();
    for (int i = 0; i < signature.length; i++) {
      key.append('_');
      key.append(constId(signature[i]));
    }
    return key.toString();
  }

  private static Annotation[] annoArray() {return new Annotation[0];}

  private static Class<?>[] classArray() {return new Class<?>[0];}
  @SuppressWarnings("unchecked")
  private static <T> Constructor<T>[] constructorArray() {return new Constructor[0];}
  private static Field[] fieldArray() {return new Field[0];}

  @SuppressWarnings("rawtypes")
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
  private static native <T> JsMemberPool<T> findMembers(int constId)
  /*-{
    return $wnd.GwtReflect.$$[constId];
  }-*/;

  private static native Method findMethod(JsMemberPool<?> pool, String id)
  /*-{
    return pool.m.hasOwnProperty(id) && pool.m[id];
  }-*/;

  private static native boolean isUnique(JavaScriptObject set, String key)
  /*-{
    return set[key] ? false : (set[key] = true);
  }-*/;

  private static Method[] methodArray() {return new Method[0];}

  private static native <T> void setMembers(int constId, JsMemberPool<T> members)
  /*-{
    $wnd.GwtReflect.$$[constId]=members;
  }-*/;

  protected JsMemberPool() {}

  public final void addAnnotation(final Annotation a, final boolean declared) {
    addAnnotation(a.annotationType().getName(), a, declared);
  }

  @SuppressWarnings("rawtypes")
  public final void addConstructor(final Constructor c) {
    addConstructor(getSignature(c.getParameterTypes()), c);
  }

  public final void addField(final Field f) {
    addField(f.getName(), f);
  }

  public final void addMethod(final Method m) {
    final String name = m.getName()+getSignature(m.getParameterTypes());
    addMethod(name, m);
  }

  @Override
  public final native <A extends Annotation> A getAnnotation(Class<A> annoCls)
  /*-{
    return this.a[annoCls.@java.lang.Class::getName()()];
  }-*/;

  @Override
  public final native Annotation[] getAnnotations()
  /*-{
    var array = @com.google.gwt.reflect.shared.JsMemberPool::annoArray()();
    for (var i in this.a) {
      if (this.a.hasOwnProperty(i))
        array.push(this.a[i]);
    }
    return array;
  }-*/;

  @Override
  public final Class<?>[] getClasses() {
    return getType().getClasses();
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public final Constructor <T> getConstructor(final Class<?> ... params) throws NoSuchMethodException {
    final String id = getSignature(params);
    JsMemberPool<? super T> pool = this;
    final Constructor method = findConstructor(pool, id);
    if (method == null) {
      pool = pool.getSuperclass();
    } else if (Modifier.isPublic(method.getModifiers())){
      return method;
    }
    throw new NoSuchMethodException("Could not find public constructor "+getType().getSimpleName()+
      "("+ReflectUtil.joinClasses(",",  params)+")");
  }

  @Override
  @SuppressWarnings({"rawtypes", "unchecked"})
  public final Constructor<T>[] getConstructors() {
    final Constructor<T>[] ctors = constructorArray();
    final JsMemberPool<? super T> pool = this;
    final JavaScriptObject set = JavaScriptObject.createObject();
    for (final Constructor declared : pool.getDeclaredConstructors()) {
      if (Modifier.isPublic(declared.getModifiers()) &&
        isUnique(set, getSignature(declared.getParameterTypes()))) {
        // javascript actually likes this; preallocating arrays doesn't save time
        ctors[ctors.length] = declared;// gwt-dev must never call this.
      }
    }
    return ctors;
  }

  @Override
  public final native <A extends Annotation> A getDeclaredAnnotation(Class<A> annoCls)
  /*-{
    var anno = this.a[annoCls.@java.lang.Class::getName()()];
    return anno && anno.declared && anno || null;
  }-*/;

  @Override
  public final native Annotation[] getDeclaredAnnotations()
  /*-{
    var array = @com.google.gwt.reflect.shared.JsMemberPool::annoArray()();
    for (var i in this.a) {
      if (this.a.hasOwnProperty(i) && this.a[i].declared)
        array.push(this.a[i]);
    }
    return array;
  }-*/;

  @Override
  public final Constructor<T> getDeclaredConstructor(final Class<?> ... params) throws NoSuchMethodException {
    final String id = getSignature(params);
    final Constructor<T> ctor = findConstructor(this, id);
    if (ctor != null) {
      return ctor;
    }
    throw new NoSuchMethodException("Could not find declared constructor "+getType().getSimpleName()+
      "("+ReflectUtil.joinClasses(",",  params)+") in "+getType());
  }

  @Override
  public final Constructor<T>[] getDeclaredConstructors() {
    final Constructor<T>[] ctors = constructorArray();
    fillConstructors(this, ctors);
    return ctors;
  }

  @Override
  public final Field getDeclaredField(final String name) throws NoSuchFieldException {
    final Field field = findField(this, name);
    if (field != null) {
      return field;
    }
    throw new NoSuchFieldException("Could not find declared field "+name+" in "+getTypeName());
  }

  @Override
  public final Field[] getDeclaredFields() {
    final Field[] fields = fieldArray();
    fillFields(this, fields);
    return fields;
  }

  @Override
  public final Method getDeclaredMethod(final String name, final Class<?> ... params) throws NoSuchMethodException {
    final String id = name + getSignature(params);
    final Method method = findMethod(this, id);
    if (method != null) {
      return method;
    }
    throw new NoSuchMethodException("Could not find declared method "+name+
      "("+ReflectUtil.joinClasses(",",  params)+") in "+getType());
  }

  @Override
  public final Method[] getDeclaredMethods() {
    final Method[] methods = methodArray();
    fillMethods(this, methods);
    return methods;
  }

  @Override
  public final Field getField(final String name) throws NoSuchFieldException {
    JsMemberPool<? super T> pool = this;
    while (pool != null) {
      final Field field = findField(pool, name);
      if (field == null) {
        pool = pool.getSuperclass();
      } else if (Modifier.isPublic(field.getModifiers())){
        return field;
      }
      else {
        break; // super classes can't match if the subclass has a lower privacy method
      }
    }
    throw new NoSuchFieldException("Could not find public field "+name+ " in "+getTypeName());
  }

  @Override
  public final Field[] getFields() {
    final Field[] fields = fieldArray();
    JsMemberPool<? super T> pool = this;
    final JavaScriptObject set = JavaScriptObject.createObject();
    while (pool != null) {
      for (final Field declared : pool.getDeclaredFields()) {
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
  public final Class<?>[] getInterfaces() {
    return getType().getInterfaces();
  }

  @Override
  public final Method getMethod(final String name, final Class<?> ... params) throws NoSuchMethodException {
    final String id = name + getSignature(params);
    JsMemberPool<? super T> pool = this;
    while (pool != null) {
      final Method method = findMethod(pool, id);
      if (method == null) {
        pool = pool.getSuperclass();
      } else if (Modifier.isPublic(method.getModifiers())){
        return method;
      }
      else {
        break; // super classes can't match if the subclass has a lower privacy method
      }
    }
    throw new NoSuchMethodException("Could not find public method "+name+
      "("+ReflectUtil.joinClasses(",",  params)+") in "+getType());
  }

  @Override
  @SuppressWarnings("rawtypes")
  public final Method[] getMethods() {
    final Method[] methods = methodArray();
    JsMemberPool<? super T> pool = this;
    final JavaScriptObject set = JavaScriptObject.createObject();
    final JsMemberPool[] all = new JsMemberPool[]{};
    if (pool.getType().isInterface()) {
      all[all.length] = this;
      for (final Class<?> iface : pool.getInterfaces()) {
        all[all.length] = getMembers(iface);
      }
    } else {
      while(pool != null) {
        all[all.length] = pool;
        pool = pool.getSuperclass();
      }
    }
    for(final JsMemberPool cls : all) {
      for (final Method declared : cls.getDeclaredMethods()) {
        if (Modifier.isPublic(declared.getModifiers()) &&
          isUnique(set, declared.getName()+getSignature(declared.getParameterTypes()))) {
          // javascript actually likes this; preallocating arrays doesn't save time
          methods[methods.length] = declared;
        }
      }
    }
    return methods;
  }

  @Override
  public final JsMemberPool<? super T> getSuperclass() {
    final Class<? super T> superClass = getType().getSuperclass();
    assert superClass != getType();
    return superClass == null ? null : getMembers(superClass);
  }

  @Override
  public final native Class<T> getType()
  /*-{
    return this.$;
  }-*/;

  public final native String getTypeName()
  /*-{
    try {
      return this.$.@java.lang.Class::getName()();
    }catch(e) {
      return "<unknown type>";
    }
  }-*/;

  private final native void addAnnotation(String key, Annotation a, boolean declared)
  /*-{
    this.a[key] = a;
    if (declared) {
      a.declared = true;
    }
  }-*/;

  @SuppressWarnings("rawtypes")
  private final native void addConstructor(String key, Constructor c)
  /*-{
    this.c[key] = c;
  }-*/;

  private final native void addField(String key, Field f)
  /*-{
    this.f[key] = f;
  }-*/;

  private final native void addMethod(String key, Method m)
  /*-{
    this.m[key] = m;
  }-*/;

}
