package com.google.gwt.reflect.shared;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.shared.GWT;

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
 *
 */
public class GwtReflect {

  /**
   * Retrieve an object from a given array using the most efficient solution for
   * a given platform. In Gwt, this does a direct int index access on the
   * supplied array object; in a JRE, it uses {@link Array#get(Object, int)}.
   * <p>
   * Note that JREs will throw {@link IndexOutOfBoundsException}, while Gwt will
   * not.
   *
   * @param array
   *          -> The array to load from
   * @param index
   *          -> The index to load from.
   * @return The object at the specified index.
   */
  public static Object arrayGet(final Object array, final int index) {
    if (GWT.isProdMode()) {
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
   *          - Any array[] instance; java or js
   * @return - The number of elements in the [].
   */
  public static int arrayLength(final Object array) {
    if (GWT.isProdMode()) {
      return jsniLength(array);
    } else {
      return Array.getLength(array);
    }
  }

  /**
   * @param c
   *          -> The class whose unique int identifier we should return
   *          <p>
   *          In JVMs, hotswapped classes that should == will have different
   *          constIds. GWT prod overrides returns a field, .constId, that we
   *          added to Class.java via supersource.
   *          <p>
   *          Note that, in both cases, the ID returned is sequential, and will
   *          be neither unique nor stable across multiple runtimes; multiple
   *          Gwt applications will reuse sequential IDs, and multiple classes
   *          loaded from different ClassLoaders or in different JVMs will
   *          assign random hashCodes to each class.
   *
   */
  public static int constId(final Class<?> c) {
    if (GWT.isClient()) {
      return JsMemberPool.constId(c);
    } else {
      return c.hashCode();
    }
  }

  /**
   *
   * In gwt dev and standard jvms, this just calls
   * cls.getConstructor(...).newInstance(...); in gwt production, this is a
   * magic method which will generate calls to new T(params);
   *
   * Note that for Gwt production to be fully optimized, you must always send
   * class literals (SomeClass.class) If you send a class reference (a
   * Class&lt;?> object), the magic method injector will be forced to generate a
   * monolithic helper class.
   *
   * In gwt production, this method will avoid generating the magic class
   * metadata.
   *
   * @param cls
   *          - The class on which to call .newInstance();
   * @param paramSignature
   *          - The constructor parameter signature. The array and it's contents
   *          must be constants.
   * @param params
   *          - The actual objects (which should be assignable to param
   *          signature).
   * @return A new instance of type T
   * @throws Throwable
   *           - Standard reflection exceptions in java vms, generator-base
   *           exceptions in js vms. InvocationTargetExceptions are unwrapped
   *           for you. This also forces you to catch Errors, which may very
   *           well be thrown by gwt, or by the jvm
   */
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
   * {@link com.google.gwt.core.ext.Generator#escape(String)}
   *
   * @return an escaped version of <code>unescaped</code>, suitable for being
   *         enclosed in double quotes in Java source
   */
  public static String escape(final String unescaped) {
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

  /**
   * Uses reflection to invoke a field getter for you; using this method will
   * net you convenience in a JRE environment, and maximal efficiency in a GWT
   * environment.
   *
   * By directly using this method, GWT will avoid creating an actual Field
   * object full of extra metadata you probably don't need, and will simply
   * generate the jsni accessor method needed to "reflectively" get objects from
   * your objects. :D
   *
   * All parameters with descriptions starting with a * will only work in GWT if
   * they are either class literals, or directly traceable to class literal
   * fields.
   *
   * @param cls
   *          - * The class on which to invoke a field get.
   * @param name
   *          - * The name of the field to get from.
   * @param inst
   *          - The instance object to get from (null for static fields)
   * @return - cls.get(Declared)Field(name).get(inst); // Checks for declared
   *         methods first Primitive return types will be boxed for you.
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
   * environment.
   *
   * By directly using this method, GWT will avoid creating an actual Field
   * object full of extra metadata you probably don't need, and will simply
   * generate the jsni accessor method needed to "reflectively" get objects from
   * your objects. :D
   *
   * All parameters with descriptions starting with a * will only work in GWT if
   * they are either class literals, or directly traceable to class literal
   * fields.
   *
   * @param cls
   *          - * The class on which to invoke a field get.
   * @param name
   *          - * The name of the field to get from.
   * @param inst
   *          - The instance object to get from (null for static fields)
   * @return - cls.get(Declared)Field(name).getBoolean(inst); // Checks for
   *         declared methods first
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
   * environment.
   *
   * By directly using this method, GWT will avoid creating an actual Field
   * object full of extra metadata you probably don't need, and will simply
   * generate the jsni accessor method needed to "reflectively" get objects from
   * your objects. :D
   *
   * All parameters with descriptions starting with a * will only work in GWT if
   * they are either class literals, or directly traceable to class literal
   * fields.
   *
   * @param cls
   *          - * The class on which to invoke a field get.
   * @param name
   *          - * The name of the field to get from.
   * @param inst
   *          - The instance object to get from (null for static fields)
   * @return - cls.get(Declared)Field(name).getByte(inst); // Checks for
   *         declared methods first
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
   * environment.
   *
   * By directly using this method, GWT will avoid creating an actual Field
   * object full of extra metadata you probably don't need, and will simply
   * generate the jsni accessor method needed to "reflectively" get objects from
   * your objects. :D
   *
   * All parameters with descriptions starting with a * will only work in GWT if
   * they are either class literals, or directly traceable to class literal
   * fields.
   *
   * @param cls
   *          - * The class on which to invoke a field get.
   * @param name
   *          - * The name of the field to get from.
   * @param inst
   *          - The instance object to get from (null for static fields)
   * @return - cls.get(Declared)Field(name).getChar(inst); // Checks for
   *         declared methods first
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
   * environment.
   *
   * By directly using this method, GWT will avoid creating an actual Field
   * object full of extra metadata you probably don't need, and will simply
   * generate the jsni accessor method needed to "reflectively" get objects from
   * your objects. :D
   *
   * All parameters with descriptions starting with a * will only work in GWT if
   * they are either class literals, or directly traceable to class literal
   * fields.
   *
   * @param cls
   *          - * The class on which to invoke a field get.
   * @param name
   *          - * The name of the field to get from.
   * @param inst
   *          - The instance object to get from (null for static fields)
   * @return - cls.get(Declared)Field(name).getDouble(inst); // Checks for
   *         declared methods first
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
   * environment.
   *
   * By directly using this method, GWT will avoid creating an actual Field
   * object full of extra metadata you probably don't need, and will simply
   * generate the jsni accessor method needed to "reflectively" get objects from
   * your objects. :D
   *
   * All parameters with descriptions starting with a * will only work in GWT if
   * they are either class literals, or directly traceable to class literal
   * fields.
   *
   * @param cls
   *          - * The class on which to invoke a field get.
   * @param name
   *          - * The name of the field to get from.
   * @param inst
   *          - The instance object to get from (null for static fields)
   * @return - cls.get(Declared)Field(name).getFloat(inst); // Checks for
   *         declared methods first
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
   * environment.
   *
   * By directly using this method, GWT will avoid creating an actual Field
   * object full of extra metadata you probably don't need, and will simply
   * generate the jsni accessor method needed to "reflectively" get objects from
   * your objects. :D
   *
   * All parameters with descriptions starting with a * will only work in GWT if
   * they are either class literals, or directly traceable to class literal
   * fields.
   *
   * @param cls
   *          - * The class on which to invoke a field get.
   * @param name
   *          - * The name of the field to get from.
   * @param inst
   *          - The instance object to get from (null for static fields)
   * @return - cls.get(Declared)Field(name).getInt(inst); // Checks for declared
   *         methods first
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
   * environment.
   *
   * By directly using this method, GWT will avoid creating an actual Field
   * object full of extra metadata you probably don't need, and will simply
   * generate the jsni accessor method needed to "reflectively" get objects from
   * your objects. :D
   *
   * All parameters with descriptions starting with a * will only work in GWT if
   * they are either class literals, or directly traceable to class literal
   * fields.
   *
   * @param cls
   *          - * The class on which to invoke a field get.
   * @param name
   *          - * The name of the field to get from.
   * @param inst
   *          - The instance object to get from (null for static fields)
   * @return - cls.get(Declared)Field(name).getLong(inst); // Checks for
   *         declared methods first
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
   * environment.
   *
   * By directly using this method, GWT will avoid creating an actual Field
   * object full of extra metadata you probably don't need, and will simply
   * generate the jsni accessor method needed to "reflectively" get objects from
   * your objects. :D
   *
   * All parameters with descriptions starting with a * will only work in GWT if
   * they are either class literals, or directly traceable to class literal
   * fields.
   *
   * @param cls
   *          - * The class on which to invoke a field get.
   * @param name
   *          - * The name of the field to get from.
   * @param inst
   *          - The instance object to get from (null for static fields)
   * @param value
   *          - The long value to set to the field. Calls:
   *          cls.get(Declared)Field(name).setLong(inst, value); // Checks for
   *          declared methods first
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
   * environment.
   *
   * By directly using this method, GWT will avoid creating an actual Field
   * object full of extra metadata you probably don't need, and will simply
   * generate the jsni accessor method needed to "reflectively" get objects from
   * your objects. :D
   *
   * All parameters with descriptions starting with a * will only work in GWT if
   * they are either class literals, or directly traceable to class literal
   * fields.
   *
   * @param cls
   *          - * The class on which to invoke a field get.
   * @param name
   *          - * The name of the field to get from.
   * @param inst
   *          - The instance object to get from (null for static fields)
   * @return - cls.get(Declared)Field(name).getShort(inst); // Checks for
   *         declared methods first
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
   * environment.
   *
   * By directly using this method, GWT will avoid creating an actual Field
   * object full of extra metadata you probably don't need, and will simply
   * generate the jsni accessor method needed to "reflectively" get objects from
   * your objects. :D
   *
   * All parameters with descriptions starting with a * will only work in GWT if
   * they are either class literals, or directly traceable to class literal
   * fields.
   *
   * @param cls
   *          - * The class on which to invoke a field get.
   * @param name
   *          - * The name of the field to get from.
   * @param inst
   *          - The instance object to get from (null for static fields)
   * @param value
   *          - The object value to set to the field. Calls:
   *          cls.get(Declared)Field(name).set(inst, value); // Checks for
   *          declared methods first Primitive boxing will NOT work here!
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
   * environment.
   *
   * By directly using this method, GWT will avoid creating an actual Field
   * object full of extra metadata you probably don't need, and will simply
   * generate the jsni accessor method needed to "reflectively" get objects from
   * your objects. :D
   *
   * All parameters with descriptions starting with a * will only work in GWT if
   * they are either class literals, or directly traceable to class literal
   * fields.
   *
   * @param cls
   *          - * The class on which to invoke a field get.
   * @param name
   *          - * The name of the field to get from.
   * @param inst
   *          - The instance object to get from (null for static fields)
   * @param value
   *          - The boolean value to set to the field. Calls:
   *          cls.get(Declared)Field(name).setBoolean(inst, value); // Checks
   *          for declared methods first
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
   * environment.
   *
   * By directly using this method, GWT will avoid creating an actual Field
   * object full of extra metadata you probably don't need, and will simply
   * generate the jsni accessor method needed to "reflectively" get objects from
   * your objects. :D
   *
   * All parameters with descriptions starting with a * will only work in GWT if
   * they are either class literals, or directly traceable to class literal
   * fields.
   *
   * @param cls
   *          - * The class on which to invoke a field get.
   * @param name
   *          - * The name of the field to get from.
   * @param inst
   *          - The instance object to get from (null for static fields)
   * @param value
   *          - The byte value to set to the field. Calls:
   *          cls.get(Declared)Field(name).setByte(inst, value); // Checks for
   *          declared methods first
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
   * environment.
   *
   * By directly using this method, GWT will avoid creating an actual Field
   * object full of extra metadata you probably don't need, and will simply
   * generate the jsni accessor method needed to "reflectively" get objects from
   * your objects. :D
   *
   * All parameters with descriptions starting with a * will only work in GWT if
   * they are either class literals, or directly traceable to class literal
   * fields.
   *
   * @param cls
   *          - * The class on which to invoke a field get.
   * @param name
   *          - * The name of the field to get from.
   * @param inst
   *          - The instance object to get from (null for static fields)
   * @param value
   *          - The char value to set to the field. Calls:
   *          cls.get(Declared)Field(name).setChar(inst, value); // Checks for
   *          declared methods first
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
   * environment.
   *
   * By directly using this method, GWT will avoid creating an actual Field
   * object full of extra metadata you probably don't need, and will simply
   * generate the jsni accessor method needed to "reflectively" get objects from
   * your objects. :D
   *
   * All parameters with descriptions starting with a * will only work in GWT if
   * they are either class literals, or directly traceable to class literal
   * fields.
   *
   * @param cls
   *          - * The class on which to invoke a field get.
   * @param name
   *          - * The name of the field to get from.
   * @param inst
   *          - The instance object to get from (null for static fields)
   * @param value
   *          - The double value to set to the field. Calls:
   *          cls.get(Declared)Field(name).setDouble(inst, value); // Checks for
   *          declared methods first
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
   * environment.
   *
   * By directly using this method, GWT will avoid creating an actual Field
   * object full of extra metadata you probably don't need, and will simply
   * generate the jsni accessor method needed to "reflectively" get objects from
   * your objects. :D
   *
   * All parameters with descriptions starting with a * will only work in GWT if
   * they are either class literals, or directly traceable to class literal
   * fields.
   *
   * @param cls
   *          - * The class on which to invoke a field get.
   * @param name
   *          - * The name of the field to get from.
   * @param inst
   *          - The instance object to get from (null for static fields)
   * @param value
   *          - The float value to set to the field. Calls:
   *          cls.get(Declared)Field(name).setFloat(inst, value); // Checks for
   *          declared methods first
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
   * environment.
   *
   * By directly using this method, GWT will avoid creating an actual Field
   * object full of extra metadata you probably don't need, and will simply
   * generate the jsni accessor method needed to "reflectively" get objects from
   * your objects. :D
   *
   * All parameters with descriptions starting with a * will only work in GWT if
   * they are either class literals, or directly traceable to class literal
   * fields.
   *
   * @param cls
   *          - * The class on which to invoke a field get.
   * @param name
   *          - * The name of the field to get from.
   * @param inst
   *          - The instance object to get from (null for static fields)
   * @param value
   *          - The int value to set to the field. Calls:
   *          cls.get(Declared)Field(name).setInt(inst, value); // Checks for
   *          declared methods first
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
   * environment.
   *
   * By directly using this method, GWT will avoid creating an actual Field
   * object full of extra metadata you probably don't need, and will simply
   * generate the jsni accessor method needed to "reflectively" get objects from
   * your objects. :D
   *
   * All parameters with descriptions starting with a * will only work in GWT if
   * they are either class literals, or directly traceable to class literal
   * fields.
   *
   * @param cls
   *          - * The class on which to invoke a field get.
   * @param name
   *          - * The name of the field to get from.
   * @param inst
   *          - The instance object to get from (null for static fields)
   * @param value
   *          - The short value to set to the field. Calls:
   *          cls.get(Declared)Field(name).setShort(inst, value); // Checks for
   *          declared methods first
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
      log(
        "Could not retrieve " + c + "(" + ReflectUtil.joinClasses(", ", params),
        e);
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
          + ReflectUtil.joinClasses(", ", params), e);
      throw new RuntimeException(e);
    }
  }

  public static Method[] getDeclaredMethods(final Class<?> c) {
    return makeAccessible(c.getDeclaredMethods());
  }

  public static Package getPackage(final String name) {
    if (GWT.isProdMode()) {
      return Package.getPackage(name);
    } else {
      return GwtReflectJre.getPackage(name);
    }
  }

  public static <T> Constructor<T> getPublicConstructor(final Class<T> c,
    final Class<?>... params) {
    try {
      return c.getConstructor(params);
    } catch (final NoSuchMethodException e) {
      log(
        "Could not retrieve " + c + "(" + ReflectUtil.joinClasses(", ", params),
        e);
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
      log(
        "Could not retrieve " + c + "." + name + "("
          + ReflectUtil.joinClasses(", ", params), e);
      throw new RuntimeException(e);
    }
  }

  public static Method[] getPublicMethods(final Class<?> c) {
    return c.getMethods();
  }

  /**
   * Uses reflection to invoke a method for you; using this method will net you
   * convenience in a JRE environment, and maximal efficiency in a GWT
   * environment.
   *
   * By directly using this method, GWT will avoid creating an actual Method
   * object full of extra metadata you probably don't need, and will simply
   * generate the jsni accessor method needed to "reflectively" access your
   * objects.
   *
   * All parameters with descriptions starting with a * will only work in GWT if
   * they are either class literals, or directly traceable to class literal
   * fields.
   *
   * @param cls
   *          - * The class on which to invoke a method.
   * @param name
   *          - * The name of the method to invoke
   * @param paramTypes
   *          - * An array of classes matching that of the method to invoke
   * @param inst
   *          - The instance object to invoke the method as (null for static
   *          methods)
   * @param params
   *          - The actual parameters to send to the object
   * @return - null for void methods, Objects or boxed primitives for everything
   *         else.
   * @throws Throwable
   *           - Throws throwable because InvocationTargetException is unwrapped
   *           for you.
   */
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
      if (!GWT.isScript() &&
        inst == null &&
        method.getDeclaringClass().isInterface() &&
        !Modifier.isAbstract(method.getModifiers())) {
        return GwtReflectJre.invokeDefaultMethod(method, params);
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
   * Ensures that a given class has all its reflection data filled in.
   *
   * A magic method injection optimizes this in production mode. You MUST send a
   * class literal for this process to work in production.
   *
   * Work is in progress to create a monolithic runtime factory, so when a
   * non-constant literal is encountered, the prod mode implementation can do a
   * runtime lookup of the type.
   *
   * A flag may be created to allow class refs to fall through and do nothing,
   * but a do-nothing call should just be erased, not worked around.
   *
   * Gwt dev and standard jvms will just call standard reflection methods, so
   * they do nothing to make a class magic.
   *
   * @param cls
   *          - The class to enhance in gwt production mode.
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
   *          - The class for which a new array will be created.
   * @param size
   *          - The size of the new array.
   * @return new T[dimension]
   */
  public static <T> T[] newArray(final Class<T> classLit, final int size) {
    return GwtReflectJre.newArray(classLit, size);
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
   *          - The class for which a new array will be created.
   * @param dim1
   *          - The size of the new array's inner dimension.
   * @param dim1
   *          - The size of the new array's outer dimension.
   * @return new T[dim1][dim2];
   */
  public static <T> T[][] newArray(final Class<T> classLit, final int dim1,
    final int dim2) {
    return GwtReflectJre.newArray(classLit, dim1, dim2);
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

  @UnsafeNativeLong
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
    GWT.log(string, e);
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

  @UnsafeNativeLong
  private static long unboxLong(final Number l) {
    return l.longValue();
  }

  private GwtReflect() {
  }

}
