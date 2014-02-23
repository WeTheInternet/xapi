/*
 * Copyright 2006 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package java.lang;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.security.ProtectionDomain;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.reflect.client.AnnotationMap;
import com.google.gwt.reflect.client.ClassMap;
import com.google.gwt.reflect.client.ConstPool;
import com.google.gwt.reflect.client.JsMemberPool;
import com.google.gwt.reflect.shared.ReflectUtil;

/**
 * Generally unsupported. This class is provided so that the GWT compiler can
 * choke down class literal references.
 * 
 * @param <T> the type of the object
 */
@SuppressWarnings("serial")
public final class Class<T>
implements java.io.Serializable, 
java.lang.reflect.GenericDeclaration, 
java.lang.reflect.Type,
java.lang.reflect.AnnotatedElement 
{
  private static final int PRIMITIVE = 0x00000001;
  private static final int INTERFACE = 0x00000002;
  private static final int ARRAY = 0x00000004;
  private static final int ENUM = 0x00000008;
  private static final String NOT_IMPLEMENTED_CORRECTLY = "You cannot call this method in gwt from a normal class" +
    " object.  You must wrap your class literal with GwtReflect.magicClass(MyClass.class) first.";
  private static final String NOT_FOUND = "Did you forget to annotate with @ReflectionStrategy methods, " +
  		"or to call GwtReflect.magicClass on your class literal?.";
  private static int index;
  
  static native String asString(int number) /*-{
    // for primitives, the seedId isn't a number, but a string like ' Z'
    return typeof(number) == 'number' ?  "S" + (number < 0 ? -number : number) : number;
  }-*/;

  /**
   * Create a Class object for an array.
   * 
   * @skip
   */
  static <T> Class<T> createForArray(String packageName, String className,
      int seedId, Class<?> componentType) {
    // Initialize here to avoid method inliner
    Class<T> clazz = new Class<T>();
    setName(clazz, packageName, className, seedId != 0 ? -seedId : 0);
    clazz.modifiers = ARRAY;
    clazz.superclass = Object.class;
    clazz.componentType = componentType;
    return clazz;
  }

  /**
   * Create a Class object for a class.
   * 
   * @skip
   */
  static <T> Class<T> createForClass(String packageName, String className,
      int seedId, Class<? super T> superclass) {
    // Initialize here to avoid method inliner
    Class<T> clazz = new Class<T>();
    setName(clazz, packageName, className, seedId);
    clazz.superclass = superclass;
    return clazz;
  }

  /**
   * Create a Class object for an enum.
   * 
   * @skip
   */
  static <T> Class<T> createForEnum(String packageName, String className,
      int seedId, Class<? super T> superclass,
      JavaScriptObject enumConstantsFunc, JavaScriptObject enumValueOfFunc) {
    // Initialize here to avoid method inliner
    Class<T> clazz = new Class<T>();
    setName(clazz, packageName, className, seedId);
    clazz.modifiers = (enumConstantsFunc != null) ? ENUM : 0;
    clazz.superclass = clazz.enumSuperclass = superclass;
    clazz.enumConstantsFunc = enumConstantsFunc;
    clazz.enumValueOfFunc = enumValueOfFunc;
    return clazz;
  }

  /**
   * Create a Class object for an interface.
   * 
   * @skip
   */
  static <T> Class<T> createForInterface(String packageName, String className) {
    // Initialize here to avoid method inliner
    Class<T> clazz = new Class<T>();
    setName(clazz, packageName, className, 0);
    clazz.modifiers = INTERFACE;
    return clazz;
  }

  /**
   * Create a Class object for a primitive.
   * 
   * @skip
   */
  static Class<?> createForPrimitive(String packageName, String className,
      int seedId) {
    // Initialize here to avoid method inliner
    Class<?> clazz = new Class<Object>();
    setName(clazz, packageName, className, seedId);
    clazz.modifiers = PRIMITIVE;
    return clazz;
  }

  /**
    * Used by {@link com.google.gwt.rpc.server.WebModePayloadSink} to create uninitialized instances.
    */
   static native JavaScriptObject getSeedFunction(Class<?> clazz) /*-{
     var func = @com.google.gwt.lang.SeedUtil::seedTable[clazz.@java.lang.Class::seedId];
     clazz = null; // HACK: prevent pruning via inlining by using param as lvalue
     return func;
   }-*/;

  public static boolean isClassMetadataEnabled() {
    // This body may be replaced by the compiler
    return false;
  }

  /**
   * null or 0 implies lack of seed function / non-instantiable type
   */
  static native boolean isInstantiable(int seedId) /*-{
    return typeof (seedId) == 'number' && seedId > 0;
  }-*/;

  /**
   * null implies pruned.
   */
  static native boolean isInstantiableOrPrimitive(int seedId) /*-{
    return seedId != null && seedId != 0;
  }-*/;

  /**
   * Install class literal into seed.prototype.clazz field such that
   * Object.getClass() returning this.clazz returns the literal. Also stores
   * seedId on class literal for looking up prototypes given a literal. This
   * is used for deRPC at the moment, but may be used to implement
   * Class.newInstance() in the future.
   */
  static native void setClassLiteral(int seedId, Class<?> clazz) /*-{
    var proto;
    clazz.@java.lang.Class::seedId = seedId;
    // String is the exception to the usual vtable setup logic
    if (seedId == 2) {
      proto = String.prototype
    } else {
      if (seedId > 0) {
        // Guarantees virtual method won't be pruned by using a JSNI ref
        // This is required because deRPC needs to call it.
        var seed = @java.lang.Class::getSeedFunction(Ljava/lang/Class;)(clazz);
        // A class literal may be referenced prior to an async-loaded vtable setup
        // For example, class literal lives in inital fragment,
        // but type is instantiated in another fragment
        if (seed) {
          proto = seed.prototype;
        } else {
          // Leave a place holder for now to be filled in by __defineSeed__ later
          seed = @com.google.gwt.lang.SeedUtil::seedTable[seedId] = function(){};
          seed.@java.lang.Object::___clazz = clazz;
          return;
        }
      } else {
        return;
      }
    }
    proto.@java.lang.Object::___clazz = clazz;
  }-*/;

  /**
   * The seedId parameter can take on the following values:
   * > 0 =>  type is instantiable class
   * < 0 => type is instantiable array
   * null => type is not instantiable
   * string => type is primitive
   */
  static void setName(Class<?> clazz, String packageName, String className,
      int seedId) {
    if (Class.isClassMetadataEnabled()||clazz.isPrimitive()) {
      clazz.pkgName = packageName.length() == 0 ? "" : packageName;
      clazz.typeName = className;
    } else {
      /*
       * The initial "" + in the below code is to prevent clazz.hashCode() from
       * being autoboxed. The class literal creation code is run very early
       * during application start up, before class Integer has been initialized.
       */
      clazz.pkgName = "";
      clazz.typeName = "Class$"
          + (isInstantiableOrPrimitive(seedId) ? asString(seedId) : "" + clazz.hashCode());
    }
    clazz.constId = clazz.remember();
    if (isInstantiable(seedId)) {
      setClassLiteral(seedId, clazz);
    }
  }
  
  /**
   * This is a magic-method hook used by Package.java; it is wired up the same as 
   * GwtReflect.magicClass, except it does not require a dependency on com.google.gwt.reflect.shared
   */
  static Class<?> magicClass(Class<?> c) {return c;}
  
  
  @SuppressWarnings("rawtypes")
  public static Class forName(String name)
    throws ClassNotFoundException{
    Class c = ConstPool.getConstPool().getClassByName(name);
    if (c == null) {
      throw new ClassNotFoundException("No class found for "+name);
    }
    return c;
  }
  @SuppressWarnings("rawtypes")
  public static Class forName(String name, boolean initialize, ClassLoader loader) 
    throws ClassNotFoundException{
    return forName(name);
  }

  JavaScriptObject enumValueOfFunc;

  int modifiers;

  protected JavaScriptObject enumConstantsFunc;
  protected String pkgName;
  protected String typeName;
  protected Class<?> componentType;
  protected Class<? super T> enumSuperclass;
  protected Class<? super T> superclass;
  private AnnotationMap annotations;
  public ClassMap<T> classData;
  public JsMemberPool<T> members;
  private int constId;

  public static <T> boolean needsEnhance(Class<T> cls) {
    if (cls.members == null) {
      // might as well init here; as soon as we return true, the class is enhanced.
      cls.members = ConstPool.getMembers(cls);
      return true;
    }
    return false;
  }
  
  public int seedId;

  /**
   * Not publicly instantiable.
   * 
   * @skip
   */
  protected Class() {
  }
  
  private native int remember()
  /*-{
    return @com.google.gwt.reflect.client.ConstPool::setClass(Ljava/lang/Class;)(this);
  }-*/;
  
  public boolean desiredAssertionStatus() {
    // This body is ignored by the JJS compiler and a new one is
    // synthesized at compile-time based on the actual compilation arguments.
    return false;
  }

  public Class<?> getComponentType() {
    return componentType;
  }

  public native T[] getEnumConstants() /*-{
    return this.@java.lang.Class::enumConstantsFunc
        && (this.@java.lang.Class::enumConstantsFunc)();
  }-*/;

  public String getName() {
    if (typeName == null) throw new NullPointerException();
    return pkgName + typeName;
  }
  public String getSimpleName() {
    return typeName;
  }

  public String getCanonicalName() {
    return pkgName + typeName.replace('$', '.');
  }

  public Class<? super T> getSuperclass() {
    if (isClassMetadataEnabled()) {
      return superclass;
    } else {
      return null;
    }
  }
  
  public Package getPackage(){
    // We don't trust our package field, because it may be elided in production compiles.
    // Using the reflection subsystem allows most types to be elided,
    // but allow selective installment of metadata like package names.
    return Package.getPackage(this);
  }

  public boolean isArray() {
    return (modifiers & ARRAY) != 0;
  }

  public boolean isEnum() {
    return (modifiers & ENUM) != 0;
  }
  
  public boolean isAnonymousClass() {
    return "".equals(getSimpleName());
  }
  

  public boolean isInterface() {
    return (modifiers & INTERFACE) != 0;
  }

  public boolean isPrimitive() {
    return (modifiers & PRIMITIVE) != 0;
  }

  public String toString() {
    return (isInterface() ? "interface " : (isPrimitive() ? "" : "class "))
        + getName();
  }

  public T newInstance()
  throws IllegalAccessException, InstantiationException {
    return classData.newInstance();
  }
  
  /**
   * Used by Enum to allow getSuperclass() to be pruned.
   */
  Class<? super T> getEnumSuperclass() {
    return enumSuperclass;
  }

  public ClassLoader getClassLoader() {
    return ClassLoader.getCallerClassLoader();
  }
  
  @SuppressWarnings("unchecked")
  public T cast(Object obj) {
    return (T) obj;
  }

  public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
    if (annotations == null)
      throw new NoSuchMethodError(NOT_IMPLEMENTED_CORRECTLY);
    return annotations.hasAnnotation(annotationClass);
  }

  public <A extends Annotation> A getAnnotation(Class<A> annotationClass) {
    if (annotations == null)
      throw new NoSuchMethodError(NOT_IMPLEMENTED_CORRECTLY);
    return annotations.getAnnotation(annotationClass);
  }

  public Annotation[] getAnnotations() {
    if (annotations == null)
      throw new NoSuchMethodError(NOT_IMPLEMENTED_CORRECTLY);
    return annotations.getAnnotations();
  }

  public Annotation[] getDeclaredAnnotations() {
    if (annotations == null)
      throw new NoSuchMethodError(NOT_IMPLEMENTED_CORRECTLY);
    return annotations.getDeclaredAnnotations();
  }
  
  public ProtectionDomain getProtectionDomain() {
    if (classData == null)
      throw new NoSuchMethodError(NOT_IMPLEMENTED_CORRECTLY);
    return classData.getProtectionDomain();
  }
  
  public Method getDeclaredMethod(String name, Class<?> ... parameterTypes)
    throws NoSuchMethodException{
    if (members == null)
      // Throw a helpful error message suggesting the client forgot to initialize this class
      // Note, we throw NoSuchMethodERROR if the method repo is null, as this means
      // we _might_ actually support this class, but it simply wasn't enhanded yet
      throw new NoSuchMethodError("Could not find "+getName()+"#"+ name+
          ": ("+ReflectUtil.joinClasses(", ", parameterTypes)+"); "+NOT_FOUND);
    // Call into our method repo; it will throw NoSuchMethodEXCEPTION,
    // as this is the correct behavior when our metho repo IS initialized,
    // but the method is legitimately missing
    return members.getDeclaredMethod(name, parameterTypes);
  }
  
  public Method getMethod(String name, Class<?> ... parameterTypes) 
    throws NoSuchMethodException{
    if (members == null)
      // Throw a helpful error message suggesting the client forgot to initialize this class
      // Note, we throw NoSuchMethodERROR if the method repo is null, as this means
      // we _might_ actually support this class, but it simply wasn't enhanced yet
      throw new NoSuchMethodError("Could not find "+getName()+"#"+ name+
          ": ("+ReflectUtil.joinClasses(", ", parameterTypes)+"); "+NOT_FOUND);
    // Call into our method repo; it will throw NoSuchMethodEXCEPTION,
    // as this is the correct behavior when our metho repo IS initialized,
    // but the method is legitimately missing

    return members.getMethod(name, parameterTypes);
  }

  public Method[] getDeclaredMethods() {
    if (members == null)
      throw new NoSuchMethodError(NOT_IMPLEMENTED_CORRECTLY);
    return members.getDeclaredMethods();
  }
  
  public Method[] getMethods() {
    if (members == null)
      throw new NoSuchMethodError(NOT_IMPLEMENTED_CORRECTLY);
    return members.getMethods();
  }
  
  public Field getDeclaredField(String name) 
  throws NoSuchFieldException
  {
    if (members == null)
      // Throw a helpful error message suggesting the client forgot to initialize this class
      // Note, we throw NoSuchFieldERROR is the field repo is null, as this means
      // we _might_ actually support this class, but it simply wasn't enhanded yet
      throw new NoSuchFieldError("Could not find "+getName()+"%"+ name+
          " "+NOT_FOUND);
    // Call into our field repo; it will throw NoSuchFieldEXCEPTION,
    // as this is the correct behavior when our field repo IS initialized,
    // but the field is legitimately missing
    return members.getDeclaredField(name);
  }
  
  public Field getField(String name) 
  throws NoSuchFieldException {
    if (members == null)
      // Throw a helpful error message suggesting the client forgot to initialize this class
      // Note, we throw NoSuchFieldERROR is the method repo is null, as this means
      // we _might_ actually support this class, but it simply wasn't enhanded yet
      throw new NoSuchFieldError("Could not find "+getName()+"%"+ name+
          " "+NOT_FOUND);
    // Call into our field repo; it will throw NoSuchFieldEXCEPTION,
    // as this is the correct behavior when our field repo IS initialized,
    // but the field is legitimately missing
    return members.getField(name);
  }
  
  public Field[] getDeclaredFields()
  {
    if (members == null)
      throw new NoSuchFieldError(NOT_IMPLEMENTED_CORRECTLY);
    return members.getDeclaredFields();
  }
  
  public Field[] getFields() {
    if (members == null)
      throw new NoSuchFieldError(NOT_IMPLEMENTED_CORRECTLY);
    return members.getFields();
  }
  
  public Constructor<T> getConstructor(Class<?> ... parameterTypes) 
  throws NoSuchMethodException {
    if (members == null)
      // Throw a helpful error message suggesting the client forgot to initialize this class
      // Note, we throw NoSuchMethodERROR is the constructor repo is null, as this means
      // we _might_ actually support this class, but it simply wasn't enhanded yet
      throw new NoSuchMethodError("Could not find "+getName()+"#<init>(" +
      		ReflectUtil.joinClasses(", ", parameterTypes)+") "+NOT_FOUND);
    // Call into our constructor repo; it will throw NoSuchMethodEXCEPTION,
    // as this is the correct behavior when our constructor repo IS initialized,
    // but the method is legitimately missing
    return members.getConstructor(parameterTypes);
  }
  
  public Constructor<T>[] getConstructors() {
    if (members == null)
      throw new NoSuchMethodError(NOT_IMPLEMENTED_CORRECTLY);
    return members.getConstructors();
  }
  
  public Constructor<T> getDeclaredConstructor(Class<?> ... parameterTypes) 
      throws NoSuchMethodException {
    if (members == null)
      // Throw a helpful error message suggesting the client forgot to initialize this class
      // Note, we throw NoSuchMethodERROR is the constructor repo is null, as this means
      // we _might_ actually support this class, but it simply wasn't enhanded yet
      throw new NoSuchMethodError("Could not find "+getName()+"#<init>(" +
          ReflectUtil.joinClasses(", ", parameterTypes)+") "+NOT_FOUND);
    // Call into our constructor repo; it will throw NoSuchMethodEXCEPTION,
    // as this is the correct behavior when our constructor repo IS initialized,
    // but the method is legitimately missing
    return members.getDeclaredConstructor(parameterTypes);
  }
  
  public Constructor<T>[] getDeclaredConstructors() {
    if (members == null)
      throw new NoSuchMethodError(NOT_IMPLEMENTED_CORRECTLY);
    return members.getDeclaredConstructors();
  }
  
  public Class<?>[] getClasses() {
    Class<?>[] list = new Class<?>[0];
    Class<?> cls = this;
    while (cls != null) {
      for (Class<?> c : cls.getDeclaredClasses()) {
        list[list.length] = c;
      }
      cls = cls.getSuperclass();
    }
    return list;
  }

  public Class<?>[] getDeclaredClasses() {
    if (classData == null) return new Class<?>[0];
    return classData.getDeclaredClasses();
  }
  
  public Class<?>[] getInterfaces() {
    if (classData == null) return new Class<?>[0];
    return classData.getInterfaces();
  }
  
  @SuppressWarnings("unchecked")
  public TypeVariable<Class<T>>[] getTypeParameters() {
//    if (getGenericSignature() != null) 
//      return (TypeVariable<Class<T>>[])getGenericInfo().getTypeParameters();
//    else
      return (TypeVariable<Class<T>>[])new TypeVariable[0];
  }
  
  @SuppressWarnings("unchecked")
  public <U> Class<? extends U> asSubclass(Class<U> clazz) {
    if (clazz.isAssignableFrom(this))
        return (Class<? extends U>) this;
    else
        throw new ClassCastException(this.toString());
  }
  
  public boolean isAssignableFrom(Class<?> cls) {
    if (cls == this)return true;
    if (isInterface()) {
      while (cls != null) {
        for (Class<?> cl : cls.getInterfaces()) {
          if (cl == this)return true;
        }
        cls = cls.getSuperclass();
      }
    }else {
      while (cls != null) {
        cls = cls.getSuperclass();
        if (cls == this)return true;
      }
    }
    return false;
  }
  
  public Method getEnclosingMethod() {
    return classData.getEnclosingMethod();
  }
  
  public Class<?> getEnclosingClass() {
    return classData.getEnclosingClass();
  }
  
  protected static native int isNumber(Class<?> cls)
  /*-{
    // yup, switch case on classes works in jsni ;)
    switch(cls) {
      case @java.lang.Byte::class:
      return 1;
      case @java.lang.Short::class:
      return 2;
      case @java.lang.Integer::class:
      return 3;
      case @java.lang.Long::class:
      return 4;
      case @java.lang.Float::class:
      return 5;
      case @java.lang.Double::class:
      return 6;
    }
    return 0;
  }-*/;

  @UnsafeNativeLong
  protected static Long boxLong(long o) {
    return new Long(o);
  }
  
  @UnsafeNativeLong
  protected static long unboxLong(Long o) {
    return o.longValue();
  }
}
