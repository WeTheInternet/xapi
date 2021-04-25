package xapi.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;

import xapi.annotation.gc.Indestructible;
import xapi.annotation.gc.OnGC;
import xapi.collect.init.AbstractMultiInitMap;
import xapi.log.X_Log;
import xapi.util.api.Destroyable;
import xapi.util.api.Destroyer;

/**
 * In order to encourage and facilitate object de-referencing,
 * the X_GC class, combined with the {@link OnGC} annotation
 * allow objects to destroy each other without necessarily knowing anything about them.
 *
 * It also allows forming "destruction chains", whereby when a root object is destroyed,
 * we can check if it's fields are destroyable and gc them too, either by annotating with OnGC,
 * or implementing the {@link Destroyable} interface.
 *
 * Not that this may NOT be what you want, in case a field is shared among multiple children.
 * Thus, GC chaining is disabled by default.
 * Default behavior is to null out fields without inspecting them.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class X_GC {

  private X_GC() {
  }

  private static final class StaticGCMethods extends AbstractMultiInitMap<String, Destroyer, Class<?>> {
    public StaticGCMethods() {
      super(PASS_THRU);
    }
    @Override
    protected Destroyer initialize(String method, Class<?> cls) {
      try{
        String[] bits = method.split("#");
        assert bits.length == 2 :
          "[ERROR] Malformed static GC method "+method+"; \n" +
              "Proper syntax is com.package.Clazz$StaticInner#methodName";
        Class<?> toLoad;
        if (bits[0].equals(cls.getName()))
          toLoad = cls;
        else {
          toLoad = Class.forName(bits[0], true, cls.getClassLoader());
        }
        final Method m = toLoad.getMethod(bits[1], Object.class);
        if (m == null){
          X_Log.warn("Could not find static gc method ",method);
          return Destroyer.NO_OP;
        }
        else {
          assert m.getTypeParameters().length == 0 :
            "Only zero-arg instance methods allowed";
          if (!m.isAccessible())
            m.setAccessible(true);
          return new Destroyer() {
            @Override
            public void destroyObject(Object toDestroy) {
              try {
                m.invoke(null, toDestroy);
              } catch (Throwable e) {
                X_Debug.maybeRethrow(e);
              }
            }
          };
        }
      } catch (Throwable e) {
        throw X_Util.rethrow(e);
      }
    }
  }

  private static final class GCMap extends AbstractMultiInitMap<Class<?>, Destroyer, OnGC>
 {

    private static final StaticGCMethods staticDestroyers = new StaticGCMethods();

    public GCMap() {
      super(CLASS_NAME);
    }
    @Override
    protected Destroyer initialize(Class<?> cls, OnGC options) {
        final ArrayList<Method> zeroArg = new ArrayList<Method>();
        for (String method : options.instanceGCMethods()) {
          try{
            Method m = cls.getMethod(method);
            if (m == null)
              m = cls.getDeclaredMethod(method);
            if (m == null)
              X_Log.warn("Could not find instance level gc method ",method);
            else {
              if (!m.isAccessible())
                m.setAccessible(true);
              assert m.getTypeParameters().length == 0 :
                "Only zero-arg instance methods allowed on instance gc methods";
              // could test for static, but we'll let that slide, as it will work...
              zeroArg.add(m);
            }
          } catch (Throwable e) {
            X_Debug.maybeRethrow(e);
          }
        }
        final ArrayList<Destroyer> fieldDestroyers = new ArrayList<Destroyer>();
        if (options.chainDeleteFields()||options.deleteInstanceFields()) {
          for (final Field f : cls.getDeclaredFields()) {
            if (shouldSkip(f))
              continue;
            try {
              if (!f.isAccessible()) {
                f.setAccessible(true);
              }
              final Destroyer setToNull = new Destroyer() {
                @Override
                public void destroyObject(Object toDestroy) {
                  try {
                    f.set(toDestroy, null);
                  } catch (Throwable e) {
                    X_Debug.maybeRethrow(e);
                  }
                }
              };
              // if the client wants us to chain gc to child fields:
              if (options.chainDeleteFields()) {
                fieldDestroyers.add(new Destroyer() {
                  @SuppressWarnings({"rawtypes","unchecked"})
                  @Override
                  public void destroyObject(Object toDestroy) {
                    Object obj;
                    try {
                      obj = f.get(toDestroy);
                      if (obj != null) {
                        if (obj instanceof Destroyable) {
                          ((Destroyable)obj).destroy();
                        }
                        Class cls = obj.getClass();
                        OnGC anno = (OnGC)cls.getAnnotation(OnGC.class);
                        if (anno != null)
                          // This object ref won't give generator details,
                          // However, it can still be made to work, by scanning
                          // for all OnGC annotations and generating code for any of them
                          destroy(cls, obj);
                      }
                    } catch (Throwable e) {
                      e.printStackTrace();
                    }
                    // also call our setToNull destroyer
                    setToNull.destroyObject(toDestroy);
                  }
                });
              } else {
                fieldDestroyers.add(setToNull);
              }
            } catch (Throwable e) {
              X_Debug.maybeRethrow(e);
            }
          }
        }
        final Destroyer instanceLevel = new Destroyer() {
          @Override
          public void destroyObject(Object toDestroy) {
            // destroy methods called
            for (Method m : zeroArg) {
              try{
                m.invoke(toDestroy);
              } catch (Throwable e){
                X_Debug.maybeRethrow(e);
              }
            }
            // fields cleared / destroy event chained
            for (Destroyer destroyer : fieldDestroyers) {
              destroyer.destroyObject(toDestroy);
            }
          }
        };
        // avoid extra work, if we can;
        // the static methods are more overhead up front
        if (options.staticGCMethods().length == 0)
          return instanceLevel;

        // We have to do reflection
        final ArrayList<Destroyer> staticLevel = new ArrayList<Destroyer>();
        for (String method : options.staticGCMethods()) {
          assert !method.contains("(") : "Do not send parantheses with method descriptions: "+method+" on "+cls;
          // share instances of a single destroyer per method, to cut down on memory duplication
          staticLevel.add(staticDestroyers.get(method, cls));
        }
        // Initialize a composite destroyer, which notifies static methods first,
        // then instance methods and finally instance fields, if enabled.
        return new Destroyer() {
          @Override
          public void destroyObject(Object toDestroy) {
            // Give static methods a crack at the object first,
            // as they are the most likely to be doing housekeeping
            // that needs to see the objects before they are destroyed.
            for (Destroyer m : staticLevel) {
              m.destroyObject(toDestroy);
            }
            instanceLevel.destroyObject(toDestroy);
          }
        };
    }
  }

  private static final GCMap destroyers = new GCMap();

  /**
   * In gwt, we can use this as a magic method to compute a destroy function,
   * but do so without bloating runtime code.  This is why you must send the class.
   *
   * If you put OnGC on a superclass, and do not wish to repeat the annotation,
   * you can send the superclass's class literal, and it will be applied instead
   * (though it will not use any overridden methods; we literally use the super classes Method objects).
   *
   * Note that you must send a the class LITERAL and not a class reference,
   * as that is what the gwt generator will use to define a GC strategy.
   *
   * In any case, the code needed to destroy the fields of the object will just be plain javascript,
   * for (key in _)if (_.hasOwnProperty(key)) delete _[key]
   *
   * Sending the class along simply means we can check for the @GC annotation, and use it to call some
   * zero-arg destroy methods for this object, and maybe even on any particular fields.
   *
   * @param classLiteral
   * @param inst
   */
  public static <T> void destroy(Class<? super T> classLiteral, T inst) {
    if (inst == null)return;
    OnGC gc = classLiteral.getAnnotation(OnGC.class);
    if (gc == null) {
      assert "true".equals(System.getProperty(
          "xapi.gcignore."+classLiteral.getName(), "false")) : "Trying to call X_GC on a class without an OnGC annotation.\n" +
      		"To suppress this assertion, set system property "+"xapi.gcignore."+classLiteral.getName()+" to true";
      return;
    }
    destroyers.get(classLiteral, gc).destroyObject(inst);
    // TODO have a flag to disable supertype gc as well.
    // Also, move this into the pre-compiled destroyer commands, instead of checking every time.
    while (classLiteral != Object.class) {
      classLiteral = classLiteral.getSuperclass();
      gc = classLiteral.getAnnotation(OnGC.class);
      if (gc != null){
        destroy(classLiteral, inst);
        return; // the above call will recurse
      }
    }
  }
  private static boolean shouldSkip(Field f) {
    return
        f.getType().isPrimitive()
        || Modifier.isFinal(f.getModifiers())
        || Modifier.isStatic(f.getModifiers())
        || f.getAnnotation(Indestructible.class) != null
        || f.isSynthetic()
        ;
  }
  public static <T> void deepDestroy(Class<? super T> classLiteral, T inst) {
    deepDestroy(classLiteral, inst, new HashSet<Object>());
    destroy(classLiteral, inst);
  }
  @SuppressWarnings("unchecked")
  public static <T> void deepDestroy(Class<? super T> classLiteral, T inst, HashSet<Object> seen) {
    for (Field f : classLiteral.getDeclaredFields()) {
      if (shouldSkip(f))continue;
      try {
        if (!f.isAccessible()) {
          f.setAccessible(true);
        }
        Object value = f.get(inst);
        if (value == null)continue;
        if (value instanceof Destroyable) {
          ((Destroyable)value).destroy();
        }
        if (value.getClass().getAnnotation(OnGC.class) != null) {
          destroy(Class.class.cast(value.getClass()), value);
        }
      } catch (Throwable e) {
        X_Debug.maybeRethrow(e);
      }
    }
  }

}
