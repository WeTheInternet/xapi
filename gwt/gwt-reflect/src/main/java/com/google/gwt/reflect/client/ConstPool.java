package com.google.gwt.reflect.client;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Vector;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.reflect.client.strategy.GwtRetention;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class ConstPool extends JavaScriptObject {

  private static final ConstPool CONSTS;

  static {
    if (GWT.isClient()) {
      CONSTS = initConstPool();
      enhance(CONSTS);
    }
    else
      CONSTS = null;
  }

  protected ConstPool() {}

  private static native ConstPool initConstPool()
  /*-{
    return {
      $:[],// enhanced classes
      $$:[],// class members
      a:[],// annotations
      c:[],// classes
      d:[],// doubles (includes float)
      e:[],// enums
      i:[],// ints (includes short, char, byte)
      l:[],// longs
      s:[],// strings
      p:{},// packages
      _a:[],// annotation arrays
      _b:[],// byte arrays
      _c:[],// char arrays
      _d:[],// double arrays
      _e:[],// enum arrays
      _f:[],// float arrays
      _i:[],// int arrays
      _j:[],// long arrays
      _l:[],// Class (type) arrays
      _o:[],// Object arrays
      _s:[],// short arrays
      _t:[],// String arrays
      _z:[]// boolan arrays
    };
  }-*/;

  // Allow the gwt compiler to enhance the const pool.
  // When eager class loading is enabled, we will hold onto
  // this method call, and use it to inject code "backwards in time",
  // and thus eagerly load properly annotated types.
  private static native ConstPool enhance(ConstPool pool)
  /*-{
    $wnd.Reflect = pool;
    return pool;
  }-*/;

  /**
   * WARNING!!
   * <p>
   * Use of this method will cause the inclusion of ALL types annotated with {@link ReflectionStrategy},
   * and all type and member dependencies declared with {@link GwtRetention}.
   * <p>
   * The {@link GWT#runAsync} class id for this split point is com.google.gwt.reflect.client.ConstPool.
   * <p>
   * This _MIGHT_ be what you want,
   * but it _WILL_ slow down your compile,
   * and cause some code bloat.
   * <p>
   * In order to mitigate some of these negative effects,
   * the code containing this monolithic collection of classes, methodes, fields, constructors,
   * annotations and constants will be put behind it's own GWT.runAsync split point.
   * <p>
   * However, you must still use some responsibility in calling this method.
   * <p>
   * The best place to call it is AFTER you've loaded your bootstrap code (exclusive code fragments),
   * and BEFORE your non-exclusive left-over fragments.  This will allow your bootstrap to remain
   * lean, and then you suck in all the types and members of core shared code into an
   * exclusive fragment, so all your non-exclusive leftovers will only contain unique code,
   * with minimal clinit() calls sprinkled around (since all shared code will already have clinit() called).
   *
   * @param callback
   */
  public static void loadConstPool(final AsyncCallback<ConstPool> callback) {
    if (GWT.isClient()) {
      com.google.gwt.core.client.GWT.runAsync(ConstPool.class, new RunAsyncCallback() {
  
        @Override
        public void onSuccess() {
          fillConstPool();
          callback.onSuccess(getConstPool());
        }
  
        @Override
        public void onFailure(Throwable reason) {
          callback.onFailure(reason);
        }
      });
    } else {
      // no jvm support yet
      callback.onSuccess(null);
    }
  }

  public static int constId(Class<?> c) {
    return c.hashCode();
    // it's unique, and good enough for jvms using maps instead of arrays.
    // GWT prod overrides this method to return a field we added to Class.
  }

  public static Object setPrimitiveArray(Class<?> componentType, Object array) {
    assert array != null;
    assert isPrimitiveArray(array);
    CONSTS.arraySet(constId(componentType), array);
    return array;
  }

  public static <T> T[] setArray(Class<?> componentType, T[] array) {
    CONSTS.arraySet(constId(componentType), array);
    return array;
  }

  public static native boolean isPrimitive(int constId)
  /*-{
    switch(@com.google.gwt.reflect.client.ConstPool::constId(Ljava/lang/Class;)(cls)) {
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

  public static Iterable<Class<?>> extractClasses(ClassLoader loader) {
    try {
      GwtReflect.magicClass(ClassLoader.class);
      Field classes = loader.getClass().getDeclaredField("classes");
      classes.setAccessible(true);
      if (GWT.isProdMode()) {
        // we have a js object of classes
        JavaScriptObject all = (JavaScriptObject)classes.get(loader);
        return fillArray(new ArrayList<Class<?>>(), all);
      } else {
        // standard jvm.  Hope this works!
        return (Vector<Class<?>>)classes.get(loader);
      }
    } catch (Exception e) {
      throw e instanceof RuntimeException ? (RuntimeException)e : new RuntimeException(e);
    }
  }

  public static <T> JsMemberPool<T> getMembers(Class<T> cls) {
    int constId = constId(cls);
    JsMemberPool<T> members = findMembers(constId);
    if (members == null) {
      members = JsMemberPool.createMemberPool(cls);
      setMembers(constId, members);
    }
    return members;
  }

  private static native <T> JsMemberPool<T> findMembers(int constId)
  /*-{
    return @com.google.gwt.reflect.client.ConstPool::CONSTS.$$[constId];
  }-*/;

  private static native <T> void setMembers(int constId, JsMemberPool<T> members)
  /*-{
    @com.google.gwt.reflect.client.ConstPool::CONSTS.$$[constId]=members;
  }-*/;

  protected static boolean isPrimitiveArray(Object array) {
    if (array == null)return false;
    Class<?> c = array.getClass().getComponentType();
    return c == null ? false : isPrimitive(constId(c));
  }

  private static native int rememberClass(int pos, Class<?> cls)
  /*-{
    @com.google.gwt.reflect.client.ConstPool::CONSTS.c[pos] = cls;
    return pos;
  }-*/;
  
  public static native int setClass(Class<?> cls)
  /*-{
    var pos = @com.google.gwt.reflect.client.ConstPool::CONSTS.c.length;
    @com.google.gwt.reflect.client.ConstPool::rememberClass(ILjava/lang/Class;)(pos, cls);
    return pos;
  }-*/;

  protected static native void setEnhancedClass(int constId, ClassMap<?> cls)
  /*-{
    @com.google.gwt.reflect.client.ConstPool::CONSTS.$[constId]=cls;
  }-*/;

  public static ConstPool getConstPool() {
    return CONSTS;
  }

  private static void fillConstPool() {
  }

  private static native Iterable<Class<?>> fillArray(ArrayList<?> list, JavaScriptObject all)
  /*-{
    for (var i in all) {
      if (all.hasOwnProperty(i))
        list.@java.util.ArrayList::add(Ljava/lang/Object;)(all[i]);
    }
    return list;
  }-*/;

  public final native <T> T getAnnotation(int id)
  /*-{
    return this.a[id];
   }-*/;

  public final native <T> Class<T> getClass(int id)
  /*-{
    return this.c[id];
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

  public final native <T> T[] getArrayObjects(int id)
  /*-{
    return this._o[id];
   }-*/;

  public final native int[] getArrayInt(int id)
  /*-{
    return this._i[id];
   }-*/;

  final <T> ClassMap<T> getClassData(Class<?> c) {
    String pkg = c.getPackage().getName();
    return getClassData(c.getPackage().getName(), c.getName().replace(pkg+".", ""));
  }

  final native <T> ClassMap<T> getClassData(String pkg, String cls)
  /*-{
    var map = this, i = pkg.indexOf('.'), nextMap;
    while (i > -1) {
      nextMap = map[pkg.substr(0, next)];
      if (nextMap == null) {
        return null;
      }
      pkg = pkg.substr(next+1);
      i = name.indexOf('.');
      map = nextMap;
    }
    return map[cls];
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

  public final native Iterable<MemberPool<?>> getAllReflectionData()
  /*-{
    return @com.google.gwt.reflect.client.ConstPool::fillArray(Ljava/util/ArrayList;Lcom/google/gwt/core/client/JavaScriptObject;)
      (@java.util.ArrayList::new()(), this.$$);
  }-*/;

  public static class Ids {
    // We can't use these "non constant" ints in switches in java,
    // but they're fair game for javascript.
    public static final int
      ID_Class = constId(Class.class),
      ID_Object = constId(Object.class),
      ID_String = constId(String.class),
      ID_Enum = constId(Enum.class),
      ID_Annotation = constId(Annotation.class),
      ID_Method = constId(Method.class),
      ID_Constructor = constId(Constructor.class),
      ID_Field = constId(Field.class),
      ID_boolean = constId(boolean.class),
      ID_byte = constId(byte.class),
      ID_char = constId(char.class),
      ID_short = constId(short.class),
      ID_int = constId(int.class),
      ID_long = constId(long.class),
      ID_float = constId(float.class),
      ID_double = constId(double.class),
      ID_Class_Array = constId(Class[].class),
      ID_Object_Array = constId(Object[].class),
      ID_String_Array = constId(String[].class),
      ID_Enum_Array = constId(Enum[].class),
      ID_Annotation_Array = constId(Annotation[].class),
      ID_Method_Array = constId(Method[].class),
      ID_Constructor_Array = constId(Constructor[].class),
      ID_Field_Array = constId(Field[].class),
      ID_boolean_Array = constId(boolean[].class),
      ID_char_Array = constId(char[].class),
      ID_short_Array = constId(short[].class),
      ID_int_Array = constId(int[].class),
      ID_long_Array = constId(long[].class),
      ID_float_Array = constId(float[].class),
      ID_double_Array = constId(double[].class)
    ;
  }

}
