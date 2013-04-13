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
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.security.ProtectionDomain;

import xapi.annotation.reflect.KeepField;
import xapi.gwt.reflect.ClassMap;
import xapi.gwt.reflect.ConstructorMap;
import xapi.gwt.reflect.FieldMap;
import xapi.gwt.reflect.MethodMap;
import xapi.util.X_String;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.shared.GWT;

/**
 * Generally unsupported. This class is provided so that the GWT compiler can
 * choke down class literal references.
 * 
 * @param <T> the type of the object
 */
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
    " object.  You must wrap your class literal with X_Reflect.magicClass(MyClass.class); first.";
  private static final String NOT_FOUND = "Did you forget to annotate with @Keep methods, " +
  		"or to call X_Reflect.magicClass on your class literal?.";

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
    * Used by {@link WebModePayloadSink} to create uninitialized instances.
    */
   static native JavaScriptObject getSeedFunction(Class<?> clazz) /*-{
     var func = @com.google.gwt.lang.SeedUtil::seedTable[clazz.@java.lang.Class::seedId];
     clazz = null; // HACK: prevent pruning via inlining by using param as lvalue
     return func;
   }-*/;

  static boolean isClassMetadataEnabled() {
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
    if (clazz.isClassMetadataEnabled()||clazz.isPrimitive()) {
      clazz.typeName = packageName + className;
    } else {
      /*
       * The initial "" + in the below code is to prevent clazz.hashCode() from
       * being autoboxed. The class literal creation code is run very early
       * during application start up, before class Integer has been initialized.
       */
      clazz.typeName = "Class$"
          + (isInstantiableOrPrimitive(seedId) ? asString(seedId) : "" + clazz.hashCode());
    }

    if (isInstantiable(seedId)) {
      setClassLiteral(seedId, clazz);
    }
  }
  
  
  public static Class forName(String name)
    throws ClassNotFoundException{
      return forName(name, true, Class.class.getClassLoader());
  }
  public static Class forName(String name, boolean initialize, ClassLoader loader) 
    throws ClassNotFoundException{
    return loader.loadClass(name);
  }

  JavaScriptObject enumValueOfFunc;

  int modifiers;

  protected JavaScriptObject enumConstantsFunc;
  protected String typeName;
  //these are regular emulated classes, and not magic classes
  protected Class<?> componentType;
  protected Class<? super T> enumSuperclass;
  protected Class<? super T> superclass;
  private MethodMap methods;
  private ConstructorMap<T> constructors;
  private FieldMap fields;
  private ClassMap<T> classData;

  public static boolean needsEnhance(Class<?> cls) {
    return cls.classData == null;
  }
  
  public int seedId;

  /**
   * Not publicly instantiable.
   * 
   * @skip
   */
  protected Class() {
  }

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
    return typeName;
  }

  public String getCanonicalName() {
    return typeName.replace('$', '.');
  }

  public Class<? super T> getSuperclass() {
    if (isClassMetadataEnabled()) {
      return superclass;
    } else {
      return null;
    }
  }
  
  public Package getPackage(){
    return Package.getPackage(this);
  }

  public boolean isArray() {
    return (modifiers & ARRAY) != 0;
  }

  public boolean isEnum() {
    return (modifiers & ENUM) != 0;
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
  throws IllegalAccessException {
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
    throw new NoSuchMethodError(NOT_IMPLEMENTED_CORRECTLY);
  }

  public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
    throw new NoSuchMethodError(NOT_IMPLEMENTED_CORRECTLY);
  }

  public Annotation[] getAnnotations() {
    throw new NoSuchMethodError(NOT_IMPLEMENTED_CORRECTLY);
  }

  public Annotation[] getDeclaredAnnotations() {
    throw new NoSuchMethodError(NOT_IMPLEMENTED_CORRECTLY);
  }
  
  public ProtectionDomain getProtectionDomain() {
    if (classData == null)
      throw new NoSuchMethodError(NOT_IMPLEMENTED_CORRECTLY);
    return classData.getProtectionDomain();
  }
  
  public Method getDeclaredMethod(String name, Class<?> ... parameterTypes)
    throws NoSuchMethodException{
    if (methods == null)
      // Throw a helpful error message suggesting the client forgot to initialize this class
      // Note, we throw NoSuchMethodERROR is the method repo is null, as this means
      // we _might_ actually support this class, but it simply wasn't enhanded yet
      throw new NoSuchMethodError("Could not find "+getName()+"#"+ name+
          ": ("+X_String.joinClasses(", ", parameterTypes)+"); "+NOT_FOUND);
    // Call into our method repo; it will throw NoSuchMethodEXCEPTION,
    // as this is the correct behavior when our metho repo IS initialized,
    // but the method is legitimately missing
    return methods.getDeclaredMethod(name, parameterTypes);
  }
  
  public Method getMethod(String name, Class<?> ... parameterTypes) 
    throws NoSuchMethodException{
    if (methods == null)
      // Throw a helpful error message suggesting the client forgot to initialize this class
      // Note, we throw NoSuchMethodERROR is the method repo is null, as this means
      // we _might_ actually support this class, but it simply wasn't enhanded yet
      throw new NoSuchMethodError("Could not find "+getName()+"#"+ name+
          ": ("+X_String.joinClasses(", ", parameterTypes)+"); "+NOT_FOUND);
    // Call into our method repo; it will throw NoSuchMethodEXCEPTION,
    // as this is the correct behavior when our metho repo IS initialized,
    // but the method is legitimately missing
    return methods.getMethod(name, parameterTypes);
  }

  public Method[] getDeclaredMethods() {
    if (methods == null)
      throw new NoSuchMethodError(NOT_IMPLEMENTED_CORRECTLY);
    return methods.getDeclaredMethods();
  }
  
  public Method[] getMethods() {
    if (methods == null)
      throw new NoSuchMethodError(NOT_IMPLEMENTED_CORRECTLY);
    return methods.getMethods();
  }
  
  public Field getDeclaredField(String name) 
  throws NoSuchFieldException
  {
    if (fields == null)
      // Throw a helpful error message suggesting the client forgot to initialize this class
      // Note, we throw NoSuchFieldERROR is the field repo is null, as this means
      // we _might_ actually support this class, but it simply wasn't enhanded yet
      throw new NoSuchFieldError("Could not find "+getName()+"%"+ name+
          " "+NOT_FOUND);
    // Call into our field repo; it will throw NoSuchFieldEXCEPTION,
    // as this is the correct behavior when our field repo IS initialized,
    // but the field is legitimately missing
    return fields.getDeclaredField(name);
  }
  
  public Field getField(String name) 
  throws NoSuchFieldException {
    if (fields == null)
      // Throw a helpful error message suggesting the client forgot to initialize this class
      // Note, we throw NoSuchFieldERROR is the method repo is null, as this means
      // we _might_ actually support this class, but it simply wasn't enhanded yet
      throw new NoSuchFieldError("Could not find "+getName()+"%"+ name+
          " "+NOT_FOUND);
    // Call into our field repo; it will throw NoSuchFieldEXCEPTION,
    // as this is the correct behavior when our field repo IS initialized,
    // but the field is legitimately missing
    return fields.getField(name);
  }
  
  public Field[] getDeclaredFields()
  {
    if (fields == null)
      throw new NoSuchFieldError(NOT_IMPLEMENTED_CORRECTLY);
    return fields.getDeclaredFields();
  }
  
  public Field[] getFields() {
    if (fields == null)
      throw new NoSuchFieldError(NOT_IMPLEMENTED_CORRECTLY);
    return fields.getFields();
  }
  
  public Constructor<T> getConstructor(Class<?> ... parameterTypes) 
  throws NoSuchMethodException {
    if (constructors == null)
      // Throw a helpful error message suggesting the client forgot to initialize this class
      // Note, we throw NoSuchMethodERROR is the constructor repo is null, as this means
      // we _might_ actually support this class, but it simply wasn't enhanded yet
      throw new NoSuchMethodError("Could not find "+getName()+"#<init>(" +
      		X_String.joinClasses(", ", parameterTypes)+") "+NOT_FOUND);
    // Call into our constructor repo; it will throw NoSuchMethodEXCEPTION,
    // as this is the correct behavior when our constructor repo IS initialized,
    // but the method is legitimately missing
    return constructors.getConstructor(parameterTypes);
  }
  
  public Constructor[] getConstructors() {
    if (constructors == null)
      throw new NoSuchMethodError(NOT_IMPLEMENTED_CORRECTLY);
    return constructors.getConstructors();
  }
  
  public Constructor<T> getDeclaredConstructor(Class<?> ... parameterTypes) 
      throws NoSuchMethodException {
    if (constructors == null)
      // Throw a helpful error message suggesting the client forgot to initialize this class
      // Note, we throw NoSuchMethodERROR is the constructor repo is null, as this means
      // we _might_ actually support this class, but it simply wasn't enhanded yet
      throw new NoSuchMethodError("Could not find "+getName()+"#<init>(" +
          X_String.joinClasses(", ", parameterTypes)+") "+NOT_FOUND);
    // Call into our constructor repo; it will throw NoSuchMethodEXCEPTION,
    // as this is the correct behavior when our constructor repo IS initialized,
    // but the method is legitimately missing
    return constructors.getDeclaredConstructor(parameterTypes);
  }
  
  public Constructor[] getDeclaredConstructors() {
    if (constructors == null)
      throw new NoSuchMethodError(NOT_IMPLEMENTED_CORRECTLY);
    return constructors.getDeclaredConstructors();
  }
  
  public Class<?>[] getClasses() {
    throw new NoSuchMethodError(NOT_IMPLEMENTED_CORRECTLY);
  }
  
  public Class<?>[] getInterfaces() {
    throw new NoSuchMethodError(NOT_IMPLEMENTED_CORRECTLY);
  }
  
  public TypeVariable<Class<T>>[] getTypeParameters() {
//    if (getGenericSignature() != null) 
//      return (TypeVariable<Class<T>>[])getGenericInfo().getTypeParameters();
//    else
      return (TypeVariable<Class<T>>[])new TypeVariable[0];
  }
  
  public <U> Class<? extends U> asSubclass(Class<U> clazz) {
    if (clazz.isAssignableFrom(this))
        return (Class<? extends U>) this;
    else
        throw new ClassCastException(this.toString());
  }
  
  public boolean isAssignableFrom(Class<?> cls) {
    if (isInterface()) {
      for (Class<?> cl : cls.getInterfaces()) {
        if (cl == this)return true;
      }
    }else {
      for (Class<?> cl : cls.getClasses()) {
        if (cl == this)return true;
      }
    }
    return false;
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
