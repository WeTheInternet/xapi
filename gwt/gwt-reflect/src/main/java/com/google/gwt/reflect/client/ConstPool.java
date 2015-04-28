package com.google.gwt.reflect.client;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.MagicMethod;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.reflect.client.strategy.GwtRetention;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.shared.ClassMap;
import com.google.gwt.reflect.shared.GwtReflect;
import com.google.gwt.reflect.shared.JsMemberPool;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

public class ConstPool extends JavaScriptObject {

  @SuppressWarnings("rawtypes")
  public static class ArrayConsts {
    // We keep these constants inside an inner class to avoid preemptive init.
    public static final Class[] EMPTY_CLASSES = new Class[0];
    public static final Object[] EMPTY_OBJECTS = new Object[0];
    public static final String[] EMPTY_STRINGS = new String[0];
    public static final Enum[] EMPTY_ENUMS = new Enum[0];
    public static final Annotation[] EMPTY_ANNOTATIONS = new Annotation[0];
    public static final Method[] EMPTY_METHODS = new Method[0];
    public static final Constructor[] EMPTY_CONSTRUCTORS = new Constructor[0];
    public static final Field[] EMPTY_FIELDS = new Field[0];

    public static final boolean[] EMPTY_booleans = new boolean[0];
    public static final byte[] EMPTY_bytes = new byte[0];
    public static final char[] EMPTY_chars = new char[0];
    public static final short[] EMPTY_shorts = new short[0];
    public static final int[] EMPTY_ints = new int[0];
    public static final long[] EMPTY_longs = new long[0];
    public static final float[] EMPTY_floats = new float[0];
    public static final double[] EMPTY_doubles = new double[0];

    public static final Boolean[] EMPTY_Booleans = new Boolean[0];
    public static final Byte[] EMPTY_Bytes = new Byte[0];
    public static final Character[] EMPTY_Characters = new Character[0];
    public static final Short[] EMPTY_Shorts = new Short[0];
    public static final Integer[] EMPTY_Integers = new Integer[0];
    public static final Long[] EMPTY_Longs = new Long[0];
    public static final Float[] EMPTY_Floats = new Float[0];
    public static final Double[] EMPTY_Doubles = new Double[0];
  }

  @SuppressWarnings("rawtypes")
  public static interface ClassConsts {
    Class<Class> CLASS_Class = Class.class;
    Class<Object> CLASS_Object = Object.class;
    Class<String> CLASS_String = String.class;
    Class<Enum> CLASS_Enum = Enum.class;
    Class CLASS_boolean = boolean.class;
    Class CLASS_byte = byte.class;
    Class CLASS_char = char.class;
    Class CLASS_short = short.class;
    Class CLASS_int = int.class;
    Class CLASS_long = long.class;
    Class CLASS_float = float.class;
    Class CLASS_double = double.class;
  }

  // this is a class and not an interface because gwt junit chokes on interface
  // here (when run in maven-surefire, that is...)
  public static final class Ids {
    // We can't use these "non constant" ints in switches in java,
    // but they're fair game for javascript.
    public static final int
    ID_Class = GwtReflect.constId(Class.class),
    ID_Object = GwtReflect.constId(Object.class),
    ID_String = GwtReflect.constId(String.class),
    ID_Enum = GwtReflect.constId(Enum.class),
    ID_Annotation = GwtReflect.constId(Annotation.class),
    ID_Method = GwtReflect.constId(Method.class),
    ID_Constructor = GwtReflect.constId(Constructor.class),
    ID_Field = GwtReflect.constId(Field.class),

    ID_boolean = GwtReflect.constId(boolean.class),
    ID_byte = GwtReflect.constId(byte.class),
    ID_char = GwtReflect.constId(char.class),
    ID_short = GwtReflect.constId(short.class),
    ID_int = GwtReflect.constId(int.class),
    ID_long = GwtReflect.constId(long.class),
    ID_float = GwtReflect.constId(float.class),
    ID_double = GwtReflect.constId(double.class),

    ID_Boolean = GwtReflect.constId(Boolean.class),
    ID_Byte = GwtReflect.constId(Byte.class),
    ID_Character = GwtReflect.constId(Character.class),
    ID_Short = GwtReflect.constId(Short.class),
    ID_Integer = GwtReflect.constId(Integer.class),
    ID_Long = GwtReflect.constId(Long.class),
    ID_Float = GwtReflect.constId(Float.class),
    ID_Double = GwtReflect.constId(Double.class),

    ID_Class_Array = GwtReflect.constId(Class[].class),
    ID_Object_Array = GwtReflect.constId(Object[].class),
    ID_String_Array = GwtReflect.constId(String[].class),
    ID_Enum_Array = GwtReflect.constId(Enum[].class),
    ID_Annotation_Array = GwtReflect.constId(Annotation[].class),
    ID_Method_Array = GwtReflect.constId(Method[].class),
    ID_Constructor_Array = GwtReflect.constId(Constructor[].class),
    ID_Field_Array = GwtReflect.constId(Field[].class),
    ID_boolean_Array = GwtReflect.constId(boolean[].class),
    ID_byte_Array = GwtReflect.constId(byte[].class),
    ID_char_Array = GwtReflect.constId(char[].class),
    ID_short_Array = GwtReflect.constId(short[].class),
    ID_int_Array = GwtReflect.constId(int[].class),
    ID_long_Array = GwtReflect.constId(long[].class),
    ID_float_Array = GwtReflect.constId(float[].class),
    ID_double_Array = GwtReflect.constId(double[].class),
    ID_Boolean_Array = GwtReflect.constId(Boolean[].class),
    ID_Byte_Array = GwtReflect.constId(Byte[].class),
    ID_Character_Array = GwtReflect.constId(Character[].class),
    ID_Short_Array = GwtReflect.constId(Short[].class),
    ID_Integer_Array = GwtReflect.constId(Integer[].class),
    ID_Long_Array = GwtReflect.constId(Long[].class),
    ID_Float_Array = GwtReflect.constId(Float[].class),
    ID_Double_Array = GwtReflect.constId(Double[].class)
    ;
  }

  /**
   * Extracts all available classes from the given classloader. In Gwt, there is
   * only one super-sourced classloader, and it contains a javascript object
   * that holds all known class objects. In JVMs, presently, we rely on the
   * presence of the Vector<Class<?>> classes inside the ClassLoader class, and
   * simply hope that we have permission to access that field reflectively.
   * <p>
   *
   * @param loader
   *          -> The classloader to read from
   * @return -> An iterable of loaded classes.
   *         <p>
   *         In both Gwt and Jvms, this Iterable will only contain classes that
   *         have been loaded at the present execution time; if there will be
   *         more classes loaded later (via runAsync or regular classloading),
   *         this method will NOT find those classes.
   *         <p>
   *         This is not a classpath scanner. If, for Gwt, you do want to load
   *         all types that are to include reflection metadata, see
   *         {@link ConstPool#loadConstPool(Callback)} instead.
   */
  @SuppressWarnings("unchecked")
  public static Iterable<Class<?>> extractClasses(final ClassLoader loader) {
    try {
      final Object classes = GwtReflect.fieldGet(ClassLoader.class, "classes",
        loader);
      if (GWT.isProdMode()) {
        // super-source ClassLoader class has a js object of classes
        final JavaScriptObject all = (JavaScriptObject) classes;
        return fillArray(new ArrayList<Class<?>>(), all);
      } else {
        // standard jvm.  Hope this works (should be a Vector<Class>)!
        return (Collection<Class<?>>) classes;
      }
    } catch (final Exception e) {
      throw e instanceof RuntimeException ? (RuntimeException) e
        : new RuntimeException(e);
    }
  }

  /**
   * @return The singleton ConstPool instance. This will currently be null in a
   *         JVM environment.
   */
  public static ConstPool getConstPool() {
    return ConstPool.CONSTS;
  }

  /**
   * Uses a quick switch statement to determine if the supplied constId is that
   * of a primitive class.
   */
  public static native boolean isPrimitive(int constId)
  /*-{
		switch (@com.google.gwt.reflect.shared.GwtReflect::constId(Ljava/lang/Class;)(cls)) {
		case @com.google.gwt.reflect.client.ConstPool.Ids::ID_byte:
		case @com.google.gwt.reflect.client.ConstPool.Ids::ID_char:
		case @com.google.gwt.reflect.client.ConstPool.Ids::ID_double:
		case @com.google.gwt.reflect.client.ConstPool.Ids::ID_boolean:
		case @com.google.gwt.reflect.client.ConstPool.Ids::ID_float:
		case @com.google.gwt.reflect.client.ConstPool.Ids::ID_int:
		case @com.google.gwt.reflect.client.ConstPool.Ids::ID_long:
		case @com.google.gwt.reflect.client.ConstPool.Ids::ID_short:
			return true;
		default:
			return false;
		}
  }-*/;

  /**
   * WARNING!!
   * <p>
   * Use of this method will cause the inclusion of ALL types annotated with
   * {@link ReflectionStrategy}, and all type and member dependencies declared
   * with {@link GwtRetention}.
   * <p>
   * The {@link GWT#runAsync} class id for this split point is
   * com.google.gwt.reflect.client.ConstPool.
   * <p>
   * This _MIGHT_ be what you want, but it _WILL_ slow down your compile, and
   * cause some code bloat.
   * <p>
   * In order to mitigate some of these negative effects, the code containing
   * this monolithic collection of classes, methods, fields, constructors,
   * annotations and constants will be put behind it's own GWT.runAsync split
   * point.
   * <p>
   * However, you must still use some responsibility in calling this method.
   * <p>
   * The best place to call it is AFTER you've loaded your bootstrap code
   * (exclusive code fragments), and BEFORE your non-exclusive left-over
   * fragments. This will allow your bootstrap to remain lean, and then you suck
   * in all the types and members of core shared code into an exclusive
   * fragment, so all your non-exclusive leftovers will only contain unique
   * code, with minimal clinit() calls sprinkled around (since all shared code
   * will already have clinit() called).
   *
   * @param callback
   */
  public static void loadConstPool(final Callback<ConstPool, Throwable> callback) {
    if (GWT.isClient()) {
      // This is a potentially very large inclusion. Put it behind split point
      com.google.gwt.core.client.GWT.runAsync(ConstPool.class,
        new RunAsyncCallback() {

          @Override
          public void onFailure(final Throwable reason) {
            callback.onFailure(reason);
          }

          @Override
          public void onSuccess() {
            // this method is a magic method filled in by the compiler
            fillConstPool();
            callback.onSuccess(getConstPool());
          }
        });
    } else {
      // no jvm support yet
      callback.onSuccess(null);
    }
  }

  /**
   * Used to retain a reference to an empty array of the given type so that we
   * can use that array as a seed for performing array reflection.
   * <p>
   * It is very unlikely that you should ever call this method manually.
   * <p>
   *
   * @param componentType
   *          -> The component type of the array
   * @param array
   *          -> An array. Preferably size 0.
   * @return -> The array you supplied.
   */
  public static <T> T[] setArray(final Class<?> componentType, final T[] array) {
    ConstPool.CONSTS.arraySet(GwtReflect.constId(componentType), array);
    return array;
  }

  /**
   * The same method call as {@link #setArray(Class, Object[])}, but with some
   * extra assertions to make sure the array does, indeed, have a primitive
   * component type.
   */
  public static Object setPrimitiveArray(final Class<?> componentType,
    final Object array) {
    assert array != null;
    assert isPrimitiveArray(array);
    ConstPool.CONSTS.arraySet(GwtReflect.constId(componentType), array);
    return array;
  }

  /**
   * Used to detmine if the array has a primitive component type or not
   */
  protected static boolean isPrimitiveArray(final Object array) {
    if (array == null) {
      return false;
    }
    final Class<?> c = array.getClass().getComponentType();
    return c == null ? false : isPrimitive(GwtReflect.constId(c));
  }

  /**
   * Used to store a global reference to the {@link ClassMap} used to supply
   * reflection objects on demand.
   */
  protected static native void setEnhancedClass(int constId, ClassMap<?> cls)
  /*-{
		@com.google.gwt.reflect.client.ConstPool::CONSTS.$[constId] = cls;
  }-*/;

  /**
   * Allow the gwt compiler to enhance the const pool. When eager class loading
   * is enabled, we will hold onto this method call, and use it to inject code
   * "backwards in time", and thus eagerly load properly annotated types.
   */
  private static native ConstPool enhance(ConstPool pool)
  /*-{
		$wnd.GwtReflect = pool;
		return pool;
  }-*/;

  private static native Iterable<Class<?>> fillArray(ArrayList<?> list,
    JavaScriptObject all)
  /*-{
		for ( var i in all) {
			if (all.hasOwnProperty(i))
				list.@java.util.ArrayList::add(Ljava/lang/Object;)(all[i]);
		}
		return list;
  }-*/;

  @MagicMethod(
    documentation = "This method is filled in with class metadata that is to be"
      + " included in the compile, but deferred until the whole app has loaded.")
  private static void fillConstPool() {
  }

  private static native ConstPool initConstPool()
  /*-{
		return $wnd.GwtReflect;
  }-*/;

  static final ConstPool CONSTS;

  static {
    if (GWT.isClient()) {
      CONSTS = initConstPool();
      enhance(CONSTS);
    }
    else {
      CONSTS = null;
    }
  }

  protected ConstPool() {
  }

  public final native Iterable<Class<?>> getAllClasses()
  /*-{
    return @com.google.gwt.reflect.client.ConstPool::fillArray(Ljava/util/ArrayList;Lcom/google/gwt/core/client/JavaScriptObject;)
      (@java.util.ArrayList::new()(), this.c);
  }-*/;

  public final native Iterable<ClassMap<?>> getAllEnhancedClasses()
  /*-{
    return @com.google.gwt.reflect.client.ConstPool::fillArray(Ljava/util/ArrayList;Lcom/google/gwt/core/client/JavaScriptObject;)
      (@java.util.ArrayList::new()(), this.$);
  }-*/;

  public final native Iterable<JsMemberPool<?>> getAllReflectionData()
  /*-{
    return @com.google.gwt.reflect.client.ConstPool::fillArray(Ljava/util/ArrayList;Lcom/google/gwt/core/client/JavaScriptObject;)
      (@java.util.ArrayList::new()(), this.$$);
  }-*/;

  public final native <T> T getAnnotation(int id)
  /*-{
		return this.a[id];
  }-*/;

  public final native int[] getArrayInt(int id)
  /*-{
		return this._i[id];
  }-*/;

  public final native <T> T[] getArrayObjects(int id)
  /*-{
		return this._o[id];
  }-*/;

  public final native <T> Class<T> getClass(int id)
  /*-{
		return this.c[id];
  }-*/;

  public final native Class<?> getClassByName(String className)
  /*-{
		return this.n[className];
  }-*/;

  public final native boolean getDouble(int id)
  /*-{
		return this.d[id];
  }-*/;

  public final native <E extends Enum<E>> E getEnum(int id)
  /*-{
		return this.e[id];
  }-*/;

  public final native int getInt(int id)
  /*-{
		return this.i[id];
  }-*/;

  @UnsafeNativeLong
  public final native long getLong(int id)
  /*-{
		return this.l[id];
  }-*/;

  public final native String getString(int id)
  /*-{
		return this.s[id];
  }-*/;

  protected final native void arraySet(int id, Object array)
  /*-{
		switch (id) {
		case @com.google.gwt.reflect.client.ConstPool.Ids::ID_Annotation:
			@com.google.gwt.reflect.client.ConstPool::CONSTS._a[id] = array;
			break;
		case @com.google.gwt.reflect.client.ConstPool.Ids::ID_byte:
			@com.google.gwt.reflect.client.ConstPool::CONSTS._b[id] = array;
			break;
		case @com.google.gwt.reflect.client.ConstPool.Ids::ID_char:
			@com.google.gwt.reflect.client.ConstPool::CONSTS._c[id] = array;
			break;
		case @com.google.gwt.reflect.client.ConstPool.Ids::ID_double:
			@com.google.gwt.reflect.client.ConstPool::CONSTS._d[id] = array;
			break;
		case @com.google.gwt.reflect.client.ConstPool.Ids::ID_Enum:
			@com.google.gwt.reflect.client.ConstPool::CONSTS._e[id] = array;
			break;
		case @com.google.gwt.reflect.client.ConstPool.Ids::ID_boolean:
			@com.google.gwt.reflect.client.ConstPool::CONSTS._z[id] = array;
			break;
		case @com.google.gwt.reflect.client.ConstPool.Ids::ID_float:
			@com.google.gwt.reflect.client.ConstPool::CONSTS._f[id] = array;
			break;
		case @com.google.gwt.reflect.client.ConstPool.Ids::ID_int:
			@com.google.gwt.reflect.client.ConstPool::CONSTS._i[id] = array;
			break;
		case @com.google.gwt.reflect.client.ConstPool.Ids::ID_long:
			@com.google.gwt.reflect.client.ConstPool::CONSTS._j[id] = array;
			break;
		case @com.google.gwt.reflect.client.ConstPool.Ids::ID_Class:
			@com.google.gwt.reflect.client.ConstPool::CONSTS._l[id] = array;
			break;
		case @com.google.gwt.reflect.client.ConstPool.Ids::ID_short:
			@com.google.gwt.reflect.client.ConstPool::CONSTS._s[id] = array;
			break;
		case @com.google.gwt.reflect.client.ConstPool.Ids::ID_String:
			@com.google.gwt.reflect.client.ConstPool::CONSTS._t[id] = array;
			break;
		default:
			@com.google.gwt.reflect.client.ConstPool::CONSTS._o[id] = array;
		}
		return array;
  }-*/;

  final <T> ClassMap<T> getClassData(final Class<?> c) {
    final String pkg = c.getPackage().getName();
    return getClassData(pkg,
      c.getName().replace(pkg + ".", ""));
  }

  final native <T> ClassMap<T> getClassData(String pkg, String cls)
  /*-{
		var map = this, i = pkg.indexOf('.'), nextMap;
		while (i > -1) {
			nextMap = map[pkg.substr(0, next)];
			if (nextMap == null) {
				return null;
			}
			pkg = pkg.substr(next + 1);
			i = name.indexOf('.');
			map = nextMap;
		}
		return map[cls];
  }-*/;

}
