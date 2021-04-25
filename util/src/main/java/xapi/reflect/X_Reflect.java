package xapi.reflect;

import xapi.annotation.compile.MagicMethod;
import xapi.fu.In1Out1;
import xapi.fu.In1Out1.In1Out1Unsafe;
import xapi.fu.Lazy;
import xapi.fu.Out1;
import xapi.fu.itr.Chain;
import xapi.fu.itr.ChainBuilder;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.log.api.LogService;
import xapi.reflect.service.ReflectionService;
import xapi.source.X_Source;
import xapi.util.X_Runtime;
import xapi.string.X_String;
import xapi.util.X_Util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static xapi.string.X_String.joinClasses;

/**
 * A set of static accessor classes to enable reflection in gwt.
 * <p>
 * Each method should be treated like GWT.create; you must send a Class literal,
 * not a class reference.
 * <p>
 * Literal: SomeClass.class<br>
 * Reference: Class<?> someClass;
 * <p>
 * Some methods will fail gracefully if you let a reference slip through. Gwt
 * production compiles will warn you if it can generate a sub-optimal solution,
 * (aka maps from reference to factory), but will throw an error if it cannot
 * deliver the functionality.
 *
 * @author James X. Nelson (james@wetheinter.net, @james)
 */
public class X_Reflect {

  static Out1<ReflectionService> reflectionService = X_Inject.singletonLazy(ReflectionService.class);
  static LogService logService;

  private static final Lazy<Class<?>> MAIN = Lazy.deferred1(X_Reflect::findMainClass);
  private static final Lazy<Boolean> IS_GRADLE = Lazy.deferred1(()->{
    final String prop = System.getProperty("xapi.gradle", null);
    // An explicit property always wins
    if ("true".equals(prop)) {
      return true;
    }
    if ("false".equals(prop)) {
      return false;
    }
    // fallback will rely on an env var that was present in gradle 5.0.
    // anybody else should either set the system property, or add the env var themselves
    String env = System.getenv("_");
    if (env != null && env.contains("gradle")) {
      return true;
    }
    return false;
  });
  private static final Lazy<Boolean> IS_MAVEN = Lazy.deferred1(()->{
    final String prop = System.getProperty("xapi.maven", null);
    // An explicit property always wins
    if ("true".equals(prop)) {
      return true;
    }
    if ("false".equals(prop)) {
      return false;
    }
    try {
      final URL loc = Thread.currentThread().getContextClassLoader().getResource("org/apache/maven/project/MavenProject.class");
      return loc != null;
    } catch (Throwable ignored) {}
    return false;
  });

  /**
   * Retrieve an object from a given array using the most efficient solution for
   * a given platform. In Gwt, this does a direct int index access on the
   * supplied array object; in a JRE, it uses {@link Array#get(Object, int)}.
   * <p>
   * Note that JREs will throw {@link IndexOutOfBoundsException}, while Gwt will
   * not.
   *
   * @param array
   * -> The array to load from
   * @param index
   * -> The index to load from.
   * @return The object at the specified index.
   */
  public static Object arrayGet(final Object array, final int index) {
    if (X_Runtime.isJavaScript()) {
      return jsniGet(array, index);
    } else {
      return Array.get(array, index);
    }
  }

  /**
   * In jvms, we defer to java.lang.reflect.Array; In Gwt, we just return the
   * .length property.
   *
   * @param array
   * - Any array[] instance; java or js
   * @return - The number of elements in the [].
   */
  public static int arrayLength(final Object array) {
    if (X_Runtime.isJavaScript()) {
      return jsniLength(array);
    } else {
      return Array.getLength(array);
    }
  }

  /**
   * @param c
   * -> The class whose unique int identifier we should return
   * <p>
   * In JVMs, hotswapped classes that should == will have different constIds.
   * GWT prod overrides returns a field, .constId, that we added to Class.java
   * via supersource.
   * <p>
   * Note that, in both cases, the ID returned is sequential, and will be
   * neither unique nor stable across multiple runtimes; multiple Gwt
   * applications will reuse sequential IDs, and multiple classes loaded from
   * different ClassLoaders or in different JVMs will assign random hashCodes to
   * each class.
   */
  public static int constId(final Class<?> c) {
    //    if (GWT.isClient()) {
    //      return JsMemberPool.constId(c);
    //    } else {
    return c.hashCode();
    //    }
  }

  /**
   * In gwt dev and standard jvms, this just calls
   * cls.getConstructor(...).newInstance(...); in gwt production, this is a
   * magic method which will generate calls to new T(params); Note that for Gwt
   * production to be fully optimized, you must always send class literals
   * (SomeClass.class) If you send a class reference (a Class&lt;?> object), the
   * magic method injector will be forced to generate a monolithic helper class.
   * In gwt production, this method will avoid generating the magic class
   * metadata.
   *
   * @param cls
   * - The class on which to call .newInstance();
   * @param paramSignature
   * - The constructor parameter signature. The array and it's contents must be
   * constants.
   * @param params
   * - The actual objects (which should be assignable to param signature).
   * @return A new instance of type T
   * @throws Throwable
   * - Standard reflection exceptions in java vms, generator-base exceptions in
   * js vms. InvocationTargetExceptions are unwrapped for you. This also forces
   * you to catch Errors, which may very well be thrown by gwt, or by the jvm
   */
  @MagicMethod(documentation="Generated by com.google.gwt.reflect.rebind.injectors.ConstructInjector")
  public static <T> T construct(final Class<? extends T> cls,
                                final Class<?>[] paramSignature, final Object... params)
  throws Throwable {
    assert isAssignable(paramSignature, params) : formatUnassignableError(cls,
        paramSignature, params);
    try {
      return makeAccessible(cls.getDeclaredConstructor(paramSignature))
          .newInstance(params);
    } catch (final InvocationTargetException e) {
      throw e.getCause();
    }
  }

  /**
   * Escapes string content to be a valid string literal. Copied directly from
   * com.google.gwt.core.ext.Generator#escape(String)
   *
   * @return an escaped version of <code>unescaped</code>, suitable for being
   * enclosed in double quotes in Java source
   */
  public static String escape(final String unescaped) {
    if (X_Runtime.isJavaScript()) {
      return nativeEscape(unescaped);
    }
    int extra = 0;
    for (int in = 0, n = unescaped.length(); in < n; ++in) {
      switch (unescaped.charAt(in)) {
        case '\0':
        case '\n':
        case '\r':
        case '\"':
        case '\\':
          ++extra;
          break;
      }
    }

    if (extra == 0) {
      return unescaped;
    }

    final char[] oldChars = unescaped.toCharArray();
    final char[] newChars = new char[oldChars.length + extra];
    for (int in = 0, out = 0, n = oldChars.length; in < n; ++in, ++out) {
      char c = oldChars[in];
      switch (c) {
        case '\0':
          newChars[out++] = '\\';
          c = '0';
          break;
        case '\n':
          newChars[out++] = '\\';
          c = 'n';
          break;
        case '\r':
          newChars[out++] = '\\';
          c = 'r';
          break;
        case '\"':
          newChars[out++] = '\\';
          c = '"';
          break;
        case '\\':
          newChars[out++] = '\\';
          c = '\\';
          break;
      }
      newChars[out] = c;
    }
    return String.valueOf(newChars);
  }

  private native static String nativeEscape(String unescaped)
  /*-{
    return unescaped.replace(/(["'\\])/g, "\\$1").replace(/\n/g,"\\n");
  }-*/;

  /**
   * Uses reflection to invoke a field getter for you; using this method will
   * net you convenience in a JRE environment, and maximal efficiency in a GWT
   * environment. By directly using this method, GWT will avoid creating an
   * actual Field object full of extra metadata you probably don't need, and
   * will simply generate the jsni accessor method needed to "reflectively" get
   * objects from your objects. :D All parameters with descriptions starting
   * with a * will only work in GWT if they are either class literals, or
   * directly traceable to class literal fields.
   *
   * @param cls
   * - * The class on which to invoke a field get.
   * @param name
   * - * The name of the field to get from.
   * @param inst
   * - The instance object to get from (null for static fields)
   * @return - cls.get(Declared)Field(name).get(inst); // Checks for declared
   * methods first Primitive return types will be boxed for you.
   * @throws SecurityException
   * @throws NoSuchFieldException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  @SuppressWarnings("unchecked")
  public static <T> T fieldGet(final Class<?> cls, final String name,
                               final Object inst) throws SecurityException, NoSuchFieldException,
                                                         IllegalArgumentException, IllegalAccessException {
    final Field field = jvmGetField(cls, name);
    return (T) field.get(inst);
  }

  /**
   * Uses reflection to invoke a field getter for you; using this method will
   * net you convenience in a JRE environment, and maximal efficiency in a GWT
   * environment. By directly using this method, GWT will avoid creating an
   * actual Field object full of extra metadata you probably don't need, and
   * will simply generate the jsni accessor method needed to "reflectively" get
   * objects from your objects. :D All parameters with descriptions starting
   * with a * will only work in GWT if they are either class literals, or
   * directly traceable to class literal fields.
   *
   * @param cls
   * - * The class on which to invoke a field get.
   * @param name
   * - * The name of the field to get from.
   * @param inst
   * - The instance object to get from (null for static fields)
   * @return - cls.get(Declared)Field(name).getBoolean(inst); // Checks for
   * declared methods first
   * @throws SecurityException
   * @throws NoSuchFieldException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  public static boolean fieldGetBoolean(final Class<?> cls, final String name,
                                        final Object inst) throws SecurityException, NoSuchFieldException,
                                                                  IllegalArgumentException, IllegalAccessException {
    final Field field = jvmGetField(cls, name);
    return field.getBoolean(inst);
  }

  /**
   * Uses reflection to invoke a field getter for you; using this method will
   * net you convenience in a JRE environment, and maximal efficiency in a GWT
   * environment. By directly using this method, GWT will avoid creating an
   * actual Field object full of extra metadata you probably don't need, and
   * will simply generate the jsni accessor method needed to "reflectively" get
   * objects from your objects. :D All parameters with descriptions starting
   * with a * will only work in GWT if they are either class literals, or
   * directly traceable to class literal fields.
   *
   * @param cls
   * - * The class on which to invoke a field get.
   * @param name
   * - * The name of the field to get from.
   * @param inst
   * - The instance object to get from (null for static fields)
   * @return - cls.get(Declared)Field(name).getByte(inst); // Checks for
   * declared methods first
   * @throws SecurityException
   * @throws NoSuchFieldException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  public static byte fieldGetByte(final Class<?> cls, final String name,
                                  final Object inst) throws SecurityException, NoSuchFieldException,
                                                            IllegalArgumentException, IllegalAccessException {
    final Field field = jvmGetField(cls, name);
    return field.getByte(inst);
  }

  /**
   * Uses reflection to invoke a field getter for you; using this method will
   * net you convenience in a JRE environment, and maximal efficiency in a GWT
   * environment. By directly using this method, GWT will avoid creating an
   * actual Field object full of extra metadata you probably don't need, and
   * will simply generate the jsni accessor method needed to "reflectively" get
   * objects from your objects. :D All parameters with descriptions starting
   * with a * will only work in GWT if they are either class literals, or
   * directly traceable to class literal fields.
   *
   * @param cls
   * - * The class on which to invoke a field get.
   * @param name
   * - * The name of the field to get from.
   * @param inst
   * - The instance object to get from (null for static fields)
   * @return - cls.get(Declared)Field(name).getChar(inst); // Checks for
   * declared methods first
   * @throws SecurityException
   * @throws NoSuchFieldException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  public static char fieldGetChar(final Class<?> cls, final String name,
                                  final Object inst) throws SecurityException, NoSuchFieldException,
                                                            IllegalArgumentException, IllegalAccessException {
    final Field field = jvmGetField(cls, name);
    return field.getChar(inst);
  }

  /**
   * Uses reflection to invoke a field getter for you; using this method will
   * net you convenience in a JRE environment, and maximal efficiency in a GWT
   * environment. By directly using this method, GWT will avoid creating an
   * actual Field object full of extra metadata you probably don't need, and
   * will simply generate the jsni accessor method needed to "reflectively" get
   * objects from your objects. :D All parameters with descriptions starting
   * with a * will only work in GWT if they are either class literals, or
   * directly traceable to class literal fields.
   *
   * @param cls
   * - * The class on which to invoke a field get.
   * @param name
   * - * The name of the field to get from.
   * @param inst
   * - The instance object to get from (null for static fields)
   * @return - cls.get(Declared)Field(name).getDouble(inst); // Checks for
   * declared methods first
   * @throws SecurityException
   * @throws NoSuchFieldException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  public static double fieldGetDouble(final Class<?> cls, final String name,
                                      final Object inst) throws SecurityException, NoSuchFieldException,
                                                                IllegalArgumentException, IllegalAccessException {
    final Field field = jvmGetField(cls, name);
    return field.getDouble(inst);
  }

  /**
   * Uses reflection to invoke a field getter for you; using this method will
   * net you convenience in a JRE environment, and maximal efficiency in a GWT
   * environment. By directly using this method, GWT will avoid creating an
   * actual Field object full of extra metadata you probably don't need, and
   * will simply generate the jsni accessor method needed to "reflectively" get
   * objects from your objects. :D All parameters with descriptions starting
   * with a * will only work in GWT if they are either class literals, or
   * directly traceable to class literal fields.
   *
   * @param cls
   * - * The class on which to invoke a field get.
   * @param name
   * - * The name of the field to get from.
   * @param inst
   * - The instance object to get from (null for static fields)
   * @return - cls.get(Declared)Field(name).getFloat(inst); // Checks for
   * declared methods first
   * @throws SecurityException
   * @throws NoSuchFieldException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  public static float fieldGetFloat(final Class<?> cls, final String name,
                                    final Object inst) throws SecurityException, NoSuchFieldException,
                                                              IllegalArgumentException, IllegalAccessException {
    final Field field = jvmGetField(cls, name);
    return field.getFloat(inst);
  }

  /**
   * Uses reflection to invoke a field getter for you; using this method will
   * net you convenience in a JRE environment, and maximal efficiency in a GWT
   * environment. By directly using this method, GWT will avoid creating an
   * actual Field object full of extra metadata you probably don't need, and
   * will simply generate the jsni accessor method needed to "reflectively" get
   * objects from your objects. :D All parameters with descriptions starting
   * with a * will only work in GWT if they are either class literals, or
   * directly traceable to class literal fields.
   *
   * @param cls
   * - * The class on which to invoke a field get.
   * @param name
   * - * The name of the field to get from.
   * @param inst
   * - The instance object to get from (null for static fields)
   * @return - cls.get(Declared)Field(name).getInt(inst); // Checks for declared
   * methods first
   * @throws SecurityException
   * @throws NoSuchFieldException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  public static int fieldGetInt(final Class<?> cls, final String name,
                                final Object inst) throws SecurityException, NoSuchFieldException,
                                                          IllegalArgumentException, IllegalAccessException {
    final Field field = jvmGetField(cls, name);
    return field.getInt(inst);
  }

  /**
   * Uses reflection to invoke a field getter for you; using this method will
   * net you convenience in a JRE environment, and maximal efficiency in a GWT
   * environment. By directly using this method, GWT will avoid creating an
   * actual Field object full of extra metadata you probably don't need, and
   * will simply generate the jsni accessor method needed to "reflectively" get
   * objects from your objects. :D All parameters with descriptions starting
   * with a * will only work in GWT if they are either class literals, or
   * directly traceable to class literal fields.
   *
   * @param cls
   * - * The class on which to invoke a field get.
   * @param name
   * - * The name of the field to get from.
   * @param inst
   * - The instance object to get from (null for static fields)
   * @return - cls.get(Declared)Field(name).getLong(inst); // Checks for
   * declared methods first
   * @throws SecurityException
   * @throws NoSuchFieldException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  public static long fieldGetLong(final Class<?> cls, final String name,
                                  final Object inst) throws SecurityException, NoSuchFieldException,
                                                            IllegalArgumentException, IllegalAccessException {
    final Field field = jvmGetField(cls, name);
    return field.getLong(inst);
  }

  /**
   * Uses reflection to invoke a field setter for you; using this method will
   * net you convenience in a JRE environment, and maximal efficiency in a GWT
   * environment. By directly using this method, GWT will avoid creating an
   * actual Field object full of extra metadata you probably don't need, and
   * will simply generate the jsni accessor method needed to "reflectively" get
   * objects from your objects. :D All parameters with descriptions starting
   * with a * will only work in GWT if they are either class literals, or
   * directly traceable to class literal fields.
   *
   * @param cls
   * - * The class on which to invoke a field get.
   * @param name
   * - * The name of the field to get from.
   * @param inst
   * - The instance object to get from (null for static fields)
   * @param value
   * - The long value to set to the field. Calls:
   * cls.get(Declared)Field(name).setLong(inst, value); // Checks for declared
   * methods first
   * @throws SecurityException
   * @throws NoSuchFieldException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  public static void fieldGetLong(final Class<?> cls, final String name,
                                  final Object inst, final long value) throws SecurityException,
                                                                              NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    final Field field = jvmGetField(cls, name);
    field.setLong(inst, value);
  }

  /**
   * Uses reflection to invoke a field getter for you; using this method will
   * net you convenience in a JRE environment, and maximal efficiency in a GWT
   * environment. By directly using this method, GWT will avoid creating an
   * actual Field object full of extra metadata you probably don't need, and
   * will simply generate the jsni accessor method needed to "reflectively" get
   * objects from your objects. :D All parameters with descriptions starting
   * with a * will only work in GWT if they are either class literals, or
   * directly traceable to class literal fields.
   *
   * @param cls
   * - * The class on which to invoke a field get.
   * @param name
   * - * The name of the field to get from.
   * @param inst
   * - The instance object to get from (null for static fields)
   * @return - cls.get(Declared)Field(name).getShort(inst); // Checks for
   * declared methods first
   * @throws SecurityException
   * @throws NoSuchFieldException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  public static short fieldGetShort(final Class<?> cls, final String name,
                                    final Object inst) throws SecurityException, NoSuchFieldException,
                                                              IllegalArgumentException, IllegalAccessException {
    final Field field = jvmGetField(cls, name);
    return field.getShort(inst);
  }

  /**
   * Uses reflection to invoke a field setter for you; using this method will
   * net you convenience in a JRE environment, and maximal efficiency in a GWT
   * environment. By directly using this method, GWT will avoid creating an
   * actual Field object full of extra metadata you probably don't need, and
   * will simply generate the jsni accessor method needed to "reflectively" get
   * objects from your objects. :D All parameters with descriptions starting
   * with a * will only work in GWT if they are either class literals, or
   * directly traceable to class literal fields.
   *
   * @param cls
   * - * The class on which to invoke a field get.
   * @param name
   * - * The name of the field to get from.
   * @param inst
   * - The instance object to get from (null for static fields)
   * @param value
   * - The object value to set to the field. Calls:
   * cls.get(Declared)Field(name).set(inst, value); // Checks for declared
   * methods first Primitive boxing will NOT work here!
   * @throws SecurityException
   * @throws NoSuchFieldException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  public static void fieldSet(final Class<?> cls, final String name,
                              final Object inst, final Object value) throws SecurityException,
                                                                            NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    final Field field = jvmGetField(cls, name);
    field.set(inst, value);
  }

  /**
   * Uses reflection to invoke a field setter for you; using this method will
   * net you convenience in a JRE environment, and maximal efficiency in a GWT
   * environment. By directly using this method, GWT will avoid creating an
   * actual Field object full of extra metadata you probably don't need, and
   * will simply generate the jsni accessor method needed to "reflectively" get
   * objects from your objects. :D All parameters with descriptions starting
   * with a * will only work in GWT if they are either class literals, or
   * directly traceable to class literal fields.
   *
   * @param cls
   * - * The class on which to invoke a field get.
   * @param name
   * - * The name of the field to get from.
   * @param inst
   * - The instance object to get from (null for static fields)
   * @param value
   * - The boolean value to set to the field. Calls:
   * cls.get(Declared)Field(name).setBoolean(inst, value); // Checks for
   * declared methods first
   * @throws SecurityException
   * @throws NoSuchFieldException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  public static void fieldSetBoolean(final Class<?> cls, final String name,
                                     final Object inst, final boolean value) throws SecurityException,
                                                                                    NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    final Field field = jvmGetField(cls, name);
    field.setBoolean(inst, value);
  }

  /**
   * Uses reflection to invoke a field setter for you; using this method will
   * net you convenience in a JRE environment, and maximal efficiency in a GWT
   * environment. By directly using this method, GWT will avoid creating an
   * actual Field object full of extra metadata you probably don't need, and
   * will simply generate the jsni accessor method needed to "reflectively" get
   * objects from your objects. :D All parameters with descriptions starting
   * with a * will only work in GWT if they are either class literals, or
   * directly traceable to class literal fields.
   *
   * @param cls
   * - * The class on which to invoke a field get.
   * @param name
   * - * The name of the field to get from.
   * @param inst
   * - The instance object to get from (null for static fields)
   * @param value
   * - The byte value to set to the field. Calls:
   * cls.get(Declared)Field(name).setByte(inst, value); // Checks for declared
   * methods first
   * @throws SecurityException
   * @throws NoSuchFieldException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  public static void fieldSetByte(final Class<?> cls, final String name,
                                  final Object inst, final byte value) throws SecurityException,
                                                                              NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    final Field field = jvmGetField(cls, name);
    field.setByte(inst, value);
  }

  /**
   * Uses reflection to invoke a field setter for you; using this method will
   * net you convenience in a JRE environment, and maximal efficiency in a GWT
   * environment. By directly using this method, GWT will avoid creating an
   * actual Field object full of extra metadata you probably don't need, and
   * will simply generate the jsni accessor method needed to "reflectively" get
   * objects from your objects. :D All parameters with descriptions starting
   * with a * will only work in GWT if they are either class literals, or
   * directly traceable to class literal fields.
   *
   * @param cls
   * - * The class on which to invoke a field get.
   * @param name
   * - * The name of the field to get from.
   * @param inst
   * - The instance object to get from (null for static fields)
   * @param value
   * - The char value to set to the field. Calls:
   * cls.get(Declared)Field(name).setChar(inst, value); // Checks for declared
   * methods first
   * @throws SecurityException
   * @throws NoSuchFieldException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  public static void fieldSetChar(final Class<?> cls, final String name,
                                  final Object inst, final char value) throws SecurityException,
                                                                              NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    final Field field = jvmGetField(cls, name);
    field.setChar(inst, value);
  }

  /**
   * Uses reflection to invoke a field setter for you; using this method will
   * net you convenience in a JRE environment, and maximal efficiency in a GWT
   * environment. By directly using this method, GWT will avoid creating an
   * actual Field object full of extra metadata you probably don't need, and
   * will simply generate the jsni accessor method needed to "reflectively" get
   * objects from your objects. :D All parameters with descriptions starting
   * with a * will only work in GWT if they are either class literals, or
   * directly traceable to class literal fields.
   *
   * @param cls
   * - * The class on which to invoke a field get.
   * @param name
   * - * The name of the field to get from.
   * @param inst
   * - The instance object to get from (null for static fields)
   * @param value
   * - The double value to set to the field. Calls:
   * cls.get(Declared)Field(name).setDouble(inst, value); // Checks for declared
   * methods first
   * @throws SecurityException
   * @throws NoSuchFieldException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  public static void fieldSetDouble(final Class<?> cls, final String name,
                                    final Object inst, final double value) throws SecurityException,
                                                                                  NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    final Field field = jvmGetField(cls, name);
    field.setDouble(inst, value);
  }

  /**
   * Uses reflection to invoke a field setter for you; using this method will
   * net you convenience in a JRE environment, and maximal efficiency in a GWT
   * environment. By directly using this method, GWT will avoid creating an
   * actual Field object full of extra metadata you probably don't need, and
   * will simply generate the jsni accessor method needed to "reflectively" get
   * objects from your objects. :D All parameters with descriptions starting
   * with a * will only work in GWT if they are either class literals, or
   * directly traceable to class literal fields.
   *
   * @param cls
   * - * The class on which to invoke a field get.
   * @param name
   * - * The name of the field to get from.
   * @param inst
   * - The instance object to get from (null for static fields)
   * @param value
   * - The float value to set to the field. Calls:
   * cls.get(Declared)Field(name).setFloat(inst, value); // Checks for declared
   * methods first
   * @throws SecurityException
   * @throws NoSuchFieldException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  public static void fieldSetFloat(final Class<?> cls, final String name,
                                   final Object inst, final float value) throws SecurityException,
                                                                                NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    final Field field = jvmGetField(cls, name);
    field.setFloat(inst, value);
  }

  /**
   * Uses reflection to invoke a field setter for you; using this method will
   * net you convenience in a JRE environment, and maximal efficiency in a GWT
   * environment. By directly using this method, GWT will avoid creating an
   * actual Field object full of extra metadata you probably don't need, and
   * will simply generate the jsni accessor method needed to "reflectively" get
   * objects from your objects. :D All parameters with descriptions starting
   * with a * will only work in GWT if they are either class literals, or
   * directly traceable to class literal fields.
   *
   * @param cls
   * - * The class on which to invoke a field get.
   * @param name
   * - * The name of the field to get from.
   * @param inst
   * - The instance object to get from (null for static fields)
   * @param value
   * - The int value to set to the field. Calls:
   * cls.get(Declared)Field(name).setInt(inst, value); // Checks for declared
   * methods first
   * @throws SecurityException
   * @throws NoSuchFieldException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  public static void fieldSetInt(final Class<?> cls, final String name,
                                 final Object inst, final int value) throws SecurityException,
                                                                            NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    final Field field = jvmGetField(cls, name);
    field.setInt(inst, value);
  }

  /**
   * Uses reflection to invoke a field setter for you; using this method will
   * net you convenience in a JRE environment, and maximal efficiency in a GWT
   * environment. By directly using this method, GWT will avoid creating an
   * actual Field object full of extra metadata you probably don't need, and
   * will simply generate the jsni accessor method needed to "reflectively" get
   * objects from your objects. :D All parameters with descriptions starting
   * with a * will only work in GWT if they are either class literals, or
   * directly traceable to class literal fields.
   *
   * @param cls
   * - * The class on which to invoke a field get.
   * @param name
   * - * The name of the field to get from.
   * @param inst
   * - The instance object to get from (null for static fields)
   * @param value
   * - The short value to set to the field. Calls:
   * cls.get(Declared)Field(name).setShort(inst, value); // Checks for declared
   * methods first
   * @throws SecurityException
   * @throws NoSuchFieldException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   */
  public static void fieldSetShort(final Class<?> cls, final String name,
                                   final Object inst, final short value) throws SecurityException,
                                                                                NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
    final Field field = jvmGetField(cls, name);
    field.setShort(inst, value);
  }

  public static <T> Constructor<T> getDeclaredConstructor(final Class<T> c,
                                                          final Class<?>... params) {
    try {
      return makeAccessible(c.getDeclaredConstructor(params));
    } catch (final NoSuchMethodException e) {
      log("Could not retrieve " + c + "(" + joinClasses(", ", params), e);
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> Constructor<T>[] getDeclaredConstructors(final Class<T> c) {
    return Constructor[].class
        .cast(makeAccessible(c.getDeclaredConstructors()));
  }

  public static Field getDeclaredField(final Class<?> c, final String name) {
    try {
      return makeAccessible(c.getDeclaredField(name));
    } catch (final NoSuchFieldException e) {
      log("Could not retrieve " + c + "." + name, e);
      throw new RuntimeException(e);
    }
  }

  public static Field[] getDeclaredFields(final Class<?> c) {
    return makeAccessible(c.getDeclaredFields());
  }

  public static Method getDeclaredMethod(final Class<?> c, final String name,
                                         final Class<?>... params) {
    try {
      return makeAccessible(c.getDeclaredMethod(name, params));
    } catch (final NoSuchMethodException e) {
      log(
          "Could not retrieve " + c + "." + name + "("
              + joinClasses(", ", params), e);
      throw new RuntimeException(e);
    }
  }

  public static Method[] getDeclaredMethods(final Class<?> c) {
    return makeAccessible(c.getDeclaredMethods());
  }

  public static Package getPackage(final String name) {
    if (X_Runtime.isJavaScript()) {
      return Package.getPackage(name);
    }
    return reflectionService.out1().getPackage(name);
  }

  public static Package getPackage(final String name, ClassLoader loader) {
    if (X_Runtime.isJavaScript()) {
      return Package.getPackage(name);
    }
    return reflectionService.out1().getPackage(name, loader);
  }

  public static <T> Constructor<T> getPublicConstructor(final Class<T> c,
                                                        final Class<?>... params) {
    try {
      return c.getConstructor(params);
    } catch (final NoSuchMethodException e) {
      log( "Could not retrieve " + c + "(" + X_String.joinClasses(", ", params), e);
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> Constructor<T>[] getPublicConstructors(final Class<T> c) {
    return Constructor[].class.cast(c.getConstructors());
  }

  public static Field getPublicField(final Class<?> c, final String name) {
    try {
      return c.getField(name);
    } catch (final NoSuchFieldException e) {
      log("Could not retrieve " + c + "." + name, e);
      throw new RuntimeException(e);
    }
  }

  public static Field[] getPublicFields(final Class<?> c) {
    return c.getFields();
  }

  public static Method getPublicMethod(final Class<?> c, final String name,
                                       final Class<?>... params) {
    try {
      return c.getMethod(name, params);
    } catch (final NoSuchMethodException e) {
      log("Could not retrieve " + c + "." + name + "(" + joinClasses(", ", params), e);
      throw new RuntimeException(e);
    }
  }

  public static Method[] getPublicMethods(final Class<?> c) {
    return c.getMethods();
  }

  /**
   * Uses reflection to invoke a method for you; using this method will net you
   * convenience in a JRE environment, and maximal efficiency in a GWT
   * environment. By directly using this method, GWT will avoid creating an
   * actual Method object full of extra metadata you probably don't need, and
   * will simply generate the jsni accessor method needed to "reflectively"
   * access your objects. All parameters with descriptions starting with a *
   * will only work in GWT if they are either class literals, or directly
   * traceable to class literal fields.
   *
   * @param cls
   * - * The class on which to invoke a method.
   * @param name
   * - * The name of the method to invoke
   * @param paramTypes
   * - * An array of classes matching that of the method to invoke
   * @param inst
   * - The instance object to invoke the method as (null for static methods)
   * @param params
   * - The actual parameters to send to the object
   * @return - null for void methods, Objects or boxed primitives for everything
   * else.
   * @throws Throwable
   * - Throws throwable because InvocationTargetException is unwrapped for you.
   */
  public static Object invokeDefaultMethod(final Class<?> cls, final String name,
                              final Class<?>[] paramTypes,
                              final Object inst, final Object... params) throws Throwable {
      Method method;
      try {
        method = makeAccessible(cls.getDeclaredMethod(name, paramTypes));
      } catch (final NoSuchMethodException e) {
        method = cls.getMethod(name, paramTypes);
      }
      return reflectionService.out1().invokeDefaultMethod(inst, method, params);
  }

  public static Object invoke(final Class<?> cls, final String name,
                              final Class<?>[] paramTypes,
                              final Object inst, final Object... params) throws Throwable {
    assert isAssignable(paramTypes, params) : formatUnassignableError(cls,
        paramTypes, params)
        + " for method named " + name;
    Method method;
    try {
      method = makeAccessible(cls.getDeclaredMethod(name, paramTypes));
    } catch (final NoSuchMethodException e) {
      method = cls.getMethod(name, paramTypes);
    }
    try {
      if (!X_Runtime.isJavaScript() &&
          inst == null &&
          method.getDeclaringClass().isInterface() &&
          !Modifier.isAbstract(method.getModifiers())) {
        return reflectionService.out1().invokeDefaultMethod(method, params);
      }
      if (method.getReturnType() == void.class) {
        method.invoke(inst, params);
        return null;
      }
      else {
        return method.invoke(inst, params);
      }
    } catch (final InvocationTargetException e) {
      throw e.getCause();
    }
  }

  public static native Object jsniGet(Object array, int index)
  /*-{
    return array[index];
  }-*/;

  public static native int jsniLength(Object array)
  /*-{
    return array.length;
  }-*/;

  /**
   * Ensures that a given class has all its reflection data filled in. A magic
   * method injection optimizes this in production mode. You MUST send a class
   * literal for this process to work in production. Work is in progress to
   * create a monolithic runtime factory, so when a non-constant literal is
   * encountered, the prod mode implementation can do a runtime lookup of the
   * type. A flag may be created to allow class refs to fall through and do
   * nothing, but a do-nothing call should just be erased, not worked around.
   * Gwt dev and standard jvms will just call standard reflection methods, so
   * they do nothing to make a class magic.
   *
   * @param cls
   * - The class to enhance in gwt production mode.
   * @return - The same class, casted to a compatible generic supertype.
   */
  @SuppressWarnings("unchecked")
  public static <T> Class<T> magicClass(final Class<? extends T> cls) {
    assert cls != null;
    return Class.class.cast(cls);
  }

  /**
   * For the time being you MUST send only class literals to this method.
   * <p>
   * Returns a new Typed[size], null-initialized and properly typed. Utilizes
   * standard java array reflection in gwt dev and plain jvms.
   * <p>
   * If you want to create multi dimensional arrays with only one dimension
   * defined, just call SomeType[][][] array = newArray(SomeType[][].class, 2);
   * <p>
   * It you need to create primitive arrays, prefer
   * {@link Array#newInstance(Class, int)}, which returns type Object, and cast
   * it yourself. Because this type signature is generic, int, double and
   * friends will auto-box. The only difference between this method and the one
   * from java.lang.reflect.Array is the return type is typesafely cast for you.
   * <p>
   * In gwt production mode, this method call is replaced with new
   * T[dimensions[0]][dimensions[1]...[];
   * <p>
   * Failing to use a class literal will currently make the compiler fail, and
   * will eventually resort to a runtime lookup in the ConstPool to get a seed
   * array to clone.
   *
   * @param classLit
   * - The class for which a new array will be created.
   * @param size
   * - The size of the new array.
   * @return new T[dimension]
   */
  public static <T> T[] newArray(final Class<T> classLit, final int size) {
    return reflectionService.out1().newArray(classLit, size);
  }

  /**
   * For the time being you MUST send only class literals to this method.
   * <p>
   * Returns a two-dimensional array, with the inner two dimensions filled in.
   * Utilizes standard java array reflection in gwt dev and plain jvms.
   * <p>
   * If you want to create complex multi-dimensional arrays this method will
   * fill in the two inner dimensions of whatever class you send (array classes
   * welcome). SomeType[][][][] array = newArray(SomeType[][].class, 4, 4);
   * <p>
   * It you need to create primitive arrays, or more complex multi-dimensional
   * arrays, prefer {@link Array#newInstance(Class, int ...)}, which returns
   * type Object, and cast it yourself. Because this type signature is generic,
   * int, double and friends will auto-box. The only difference between this
   * method and the one from java.lang.reflect.Array is the return type is
   * typesafely cast for you.
   * <p>
   * In gwt production mode, this method call is replaced with new
   * T[dimensions[0]][dimensions[1]...[];
   * <p>
   * Failing to use a class literal will currently make the compiler fail, and
   * will eventually resort to a runtime lookup in the ConstPool to get a seed
   * array to clone.
   *
   * @param classLit
   * - The class for which a new array will be created.
   * @param dim1
   * - The size of the new array's inner dimension.
   * @param dim1
   * - The size of the new array's outer dimension.
   * @return new T[dim1][dim2];
   */
  public static <T, R extends T> T[][] newArray(final Class<R> classLit, final int dim1, final int dim2) {
    return reflectionService.out1().newArray(classLit, dim1, dim2);
  }

  public static <T extends Throwable> T doThrow(final T exception) throws T {
    throw exception;
  }

  private static int assignableDepth(final Class<?>[] paramSignature,
                                     final Object[] params) {
    if (paramSignature.length != params.length) {
      return 0;
    }
    for (int i = paramSignature.length; i-- > 0;) {
      final Class<?> sig = paramSignature[i];
      final Object param = params[i];
      if (sig.isPrimitive()) {
        if (param == null) {
          return i;
        }
        // Gwt dev gets a crack at handling boxing.
        if (sig.getName().equalsIgnoreCase(param.getClass().getSimpleName())) {
          continue;
        }
        if (sig == int.class && param instanceof Integer) {
          continue;
        }
        if (sig == char.class && param instanceof Character) {
          continue;
        }
        return i;
      } else {
        if (!sig.isAssignableFrom(param.getClass())) {
          return i;
        }
      }
    }
    return -1;
  }

  //js: @UnsafeNativeLong
  private static Long boxLong(final long l) {
    return new Long(l);
  }

  private static String formatUnassignableError(final Class<?> cls,
                                                final Class<?>[] paramSignature, final Object... params) {
    final int depth = assignableDepth(paramSignature, params);
    return "Unassignable parameter signature for class "
        + cls.getName()
        + "; mismatch on parameter "
        + depth
        + "\n Signature type was "
        + paramSignature[depth].getName()
        + "; object was "
        +
        (params[depth] == null ? "null" : " a "
            + params[depth].getClass().getName() + " : " + params[depth]);
  }

  private static boolean isAssignable(final Class<?>[] paramSignature,
                                      final Object[] params) {
    return assignableDepth(paramSignature, params) == -1;
  }

  private static Field jvmGetField(Class<?> cls, final String name)
  throws NoSuchFieldException {
    try {
      // Prefer the declared, unaccesible field
      return makeAccessible(cls.getDeclaredField(name));
    } catch (final NoSuchFieldException e0) {
      try {
        // Next, check superclasses for public fields
        return cls.getField(name);
      } catch (final NoSuchFieldException e1) {
        // Finally, check all superclasses for private/protected fields
        while (true) {
          cls = cls.getSuperclass();
          if (cls == Object.class || cls == null) {
            throw new NoSuchFieldException("No field " + name + " in " + cls);
          }
          try {
            return makeAccessible(cls.getDeclaredField(name));
          } catch (final NoSuchFieldException e2) {
            continue;
          }
        }
      }
    }
  }

  private static void log(final String string, final Throwable e) {
    if (logService == null) {
      X_Log.error(X_Reflect.class, string, e);
    } else {
      logService.error(X_Reflect.class, string, e);
    }
  }

  private static <T extends AccessibleObject> T makeAccessible(final T member) {
    // TODO use security manager
    if (!member.isAccessible()) {
      member.setAccessible(true);
    }
    return member;
  }

  private static <T extends AccessibleObject> T[] makeAccessible(
      final T[] members) {
    for (final T member : members) {
      makeAccessible(member);
    }
    return members;
  }

  private static void nullCheck(final Object o) {
    if (o == null) {
      throw new NullPointerException("Null is not allowed");
    }
  }

  //js: @UnsafeNativeLong
  private static long unboxLong(final Number l) {
    return l.longValue();
  }

  private X_Reflect() {
  }

  public static String className(Object value) {
    return value == null ? "<null>" : value.getClass().getCanonicalName();
  }

  public static <T> Class<? extends T> getGenericAsClass(Object from) {
    return getGenericAsClass(from, 0);
  }

  public static <T> Class<? extends T> getGenericAsClass(Object from, int position) {
    assert from != null : "Do not send null objects to X_Reflect.getGenericsAsClass";
    final TypeVariable<? extends Class<?>>[] params = from.getClass().getTypeParameters();
    assert params.length > position : "Requested a generic type from position " + position +" of type " + from.getClass()+", but no type variable found on object " + from;
    return (Class<? extends T>) params[position].getGenericDeclaration();
  }

  public static String getFileLoc(Class<?> mainClass) {
    assert mainClass != null : "Do not send null class to X_Reflect.getFileLoc";
    URL loc = null;
    final ProtectionDomain domain = mainClass.getProtectionDomain();
    if (domain != null) {
      final CodeSource source = mainClass.getProtectionDomain().getCodeSource();
      if (source != null) {
        loc = source.getLocation();
      }
    }

    String suffix = getOuterName(mainClass).replace('.', '/')+".class";
    if (loc == null) {
      ClassLoader cl = mainClass.getClassLoader();
      if (cl == null) {
        cl = Thread.currentThread().getContextClassLoader();
      }
      loc = cl.getResource(suffix);
    }
    boolean isJar = loc.getProtocol().equals("jar") || loc.toExternalForm().contains("jar!");
    if (isJar) {
      // When the main class is in a jar, we need to make sure that jar is on the classpath
      String jar = loc.toExternalForm().replace("jar:", "").split("jar!")[0] + "jar";
      return jar;
    } else {
      // location is a source path; add it directly
      return loc.toExternalForm().replace("file:", "").replace(suffix, "");
    }
  }

  private static String getOuterName(Class<?> cls) {
    while (cls.getEnclosingClass() != null) {
      cls = cls.getEnclosingClass();
    }
    return cls.getCanonicalName();
  }

  private static String getCanonicalName(Class<?> cls) {
    ChainBuilder<String> b = Chain.startChain();
    while (cls.getEnclosingClass() != null) {
      b.insert(cls.getSimpleName());
      cls = cls.getEnclosingClass();
    }
    return cls.getCanonicalName()
        + b.join(".", "$", "");
  }

  public static <T> T mostDerived(T ours, T theirs) {
      final Class<?> ourClass = ours.getClass();
      final Class<?> theirClass = theirs.getClass();
      if (ourClass == theirClass) {
        return ours;
      }
      Class<?> seek = ourClass;
      while (seek != null && seek != Object.class) {
        if (seek == theirClass) {
          return ours;
        }
        seek = seek.getSuperclass();
      }
      seek = theirClass;
      while (seek != null && seek != Object.class) {
        if (seek == ourClass) {
          return theirs;
        }
        seek = seek.getSuperclass();
      }
      // No luck; return null, since these types are not assignable.
      // good luck if you are using (bad) reflection proxies instead of real objects :-)
      // actually, your course of action is to change the code which calls this method.
      return null;
    }

  public static String getSourceLoc(Class<?> cls) {
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    final String srcName = cls.getName().replace('.', '/') + ".java";
    URL sourceFile = cl.getResource(srcName);
    String loc;
    if (sourceFile == null) {
      loc = X_Reflect.getFileLoc(cls);
    } else {
      loc = sourceFile.toExternalForm().replace("file:", "").replace(srcName, "");
      if (loc.endsWith("classes/")) {
        // there might be a source file in our classes folder,
        // so keep looking a file based
        try {
          final Enumeration<URL> choices = cl.getResources(srcName);
          while (choices.hasMoreElements()) {
            sourceFile = choices.nextElement();
            loc = sourceFile.toExternalForm().replace("file:", "").replace(srcName, "");
            if (!loc.endsWith("classes/") && !loc.contains("rt.jar")) {
              // it doesn't end in classes; it's probably the source we are looking for
              return loc;
            }
          }
        } catch (IOException ignored) {}
      }

    }
    if (loc != null && loc.contains("classes/")) {
      // TODO: use CompilerService somehow, to get configurable source directories...
      String fixed = X_Source.rebase(loc, "src/main/java", "src/test/java");
      if (fixed.equals(loc)) {
        // old, hacky version that likely only works in maven...
        int ind = loc.lastIndexOf('/', loc.length() - 8);
        if (ind != -1) {
          loc = loc.substring(0, ind);
        }
        // handle target/test-classes suffix (chop back to trailing \)
        if (loc.endsWith("/target")) {
          loc = loc.substring(0, loc.length() - 7);
        }
        File f = new File(loc, "src/main/java/" + srcName);
        if (f.exists()) {
          loc = f.getAbsolutePath();
        }
      } else {
        File f = new File(fixed, srcName);
        if (f.exists()) {
          loc = f.getAbsolutePath();
        }
      }
    }
    return loc;
  }

    public static Class<?> getRootClass(In1Out1Unsafe<StackTraceElement, Boolean> filter) {
      // find the main thread, then pick the deepest stack element that matches the filter
      final Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
      for (Entry<Thread, StackTraceElement[]> trace : traces.entrySet()) {
        if ("main".equals(trace.getKey().getName())) {
          // Using a thread named main is best...
          final StackTraceElement[] els = trace.getValue();
          int i = els.length - 1;
          StackTraceElement best = els[--i];
          String cls = best.getClassName();
          while (i > 0 && isSystemClass(cls)) {
            // if the main class is likely an ide,
            // then we should look deeper...
            while (i-- > 0) {
              if (filter.io(els[i])) {
                best = els[i];
                cls = best.getClassName();
                break;
              }
            }
          }
          try {
            Class mainClass = Class.forName(best.getClassName());
            return mainClass;
          } catch (ClassNotFoundException e) {
            throw X_Util.rethrow(e);
          }
        }
      }
      return null;
    }
    public static Class<?> getMainClass() {
      return MAIN.out1();
    }
    private static Class<?> findMainClass() {
        return findMainClass(In1Out1.<StackTraceElement, Boolean>returnTrue().unsafeIn1Out1());
    }
    private static Class<?> findMainClass(In1Out1Unsafe<StackTraceElement, Boolean> filter) {
      // finds any loadable class that is present as a stack trace w/ method `name`.
      final Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
      Class mainClass = null;
      for (Entry<Thread, StackTraceElement[]> trace : traces.entrySet()) {
        if ("main".equals(trace.getKey().getName())
            // TODO: put this looser check back ...later
//           || (trace.getKey().getName().contains("worker") && trace.getKey().getThreadGroup() != null && "main".equals(trace.getKey().getThreadGroup().getName()))
        ) {
          // Using a thread named main is best...
          final StackTraceElement[] els = trace.getValue();
          int i = els.length;
          StackTraceElement best = els[--i];
          if (!filter.io(best)) {
            continue;
          }
          String cls = best.getClassName();
          // loop until we have a main that isn't deemed to be a system class...
          while (i > 0 && isSystemClass(cls)) {
            // if the main class is likely an ide,
            // then we should look higher...
            while (i-- > 0) {
              if (filter.io(els[i]) && "main".equals(els[i].getMethodName())) {
                best = els[i];
                cls = best.getClassName();
                break;
              }
            }
          }
          if (isSystemClass(cls)) {
            i = els.length - 1;
            if (filter.io(els[i])) {
              best = els[i];
            }
            while (isSystemClass(cls) && i-- > 0) {
              if (filter.io(els[i])) {
                best = els[i];
              }
              cls = best.getClassName();
            }
          }
          boolean shouldUse = !isSystemClass(best.getClassName());
          try {
            mainClass = Thread.currentThread().getContextClassLoader().loadClass(best.getClassName());
            if (shouldUse) {
              return mainClass;
            }
          } catch (NoClassDefFoundError|ClassNotFoundException e) {
            try {
              mainClass = X_Reflect.class.getClassLoader().loadClass(best.getClassName());
              if (shouldUse) {
                return mainClass;
              }
            } catch (NoClassDefFoundError|ClassNotFoundException e1) {
              if (shouldUse) {
                X_Log.warn(X_Reflect.class, "Found ", best.getClassName(), "but could not load it from any available classloader");
              }
            }
          }
        }
      }
      final StackTraceElement[] els = Thread.currentThread().getStackTrace();
      for (int i = els.length; i-->0;) {
        if (!isSystemClass(els[i].getClassName())) {
          try {
            return Thread.currentThread().getContextClassLoader().loadClass(els[i].getClassName());
          } catch (NoClassDefFoundError|ClassNotFoundException e) {
            try {
              return X_Reflect.class.getClassLoader().loadClass(els[i].getClassName());
            } catch (NoClassDefFoundError|ClassNotFoundException e1) {
              X_Log.warn(X_Reflect.class, "Found ", els[i], "but could not load it from any available classloader");
            }
          }

        }
      }

      return mainClass;
    }

    private static boolean isSystemClass(String cls) {
      // TODO: this has gotten crazy.  This should be configurable w/ a nice default.
      return cls.startsWith("java.") ||
          cls.startsWith("sun.") ||
          cls.startsWith("com.sun.") ||
          cls.startsWith("com.esotericsoftware.") ||
          cls.startsWith("org.apache.maven.") ||
          cls.contains(".intellij.") ||
          cls.startsWith("org.spockframework") ||
          cls.startsWith("org.junit") ||
          cls.startsWith("junit.") ||
          cls.startsWith("cucumber.") ||
          cls.contains(".eclipse") ||
          cls.contains("org.gradle") ||
          "xapi.dev.impl.ReflectiveMavenLoader".equals(cls) ||
          "xapi.dev.impl.MavenLoaderThread".equals(cls) ||
          cls.contains("netbeans");
    }

  public static Class<?> rebase(Class<?> clazz, ClassLoader toLoader) {
    if (clazz.isArray()) {
      final Class<?> component = rebase(clazz.getComponentType(), toLoader);
      return X_Reflect.newArray(component, 0).getClass();
    }
    try {
      return toLoader.loadClass(clazz.getName());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean isParentOrSelf(ClassLoader loader) {
    ClassLoader self = Thread.currentThread().getContextClassLoader();
    if (self == loader) {
      return true;
    }
    while (self != null) {
      self = self.getParent();
      if (self == loader) {
        return true;
      }
    }
    return false;
  }

  public static boolean hasPublicOverloads(Class<?> cls) {
    Set<String> seen = new HashSet<>();
    for (Method method : cls.getMethods()) {
      if (!seen.add(method.getName())) {
        return false;
      }
    }
    return true;
  }

  public static boolean isGradle() {
    return IS_GRADLE.out1();
  }

  public static boolean isMaven() {
    return IS_MAVEN.out1();
  }
}
