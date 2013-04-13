package xapi.reflect;

import javax.inject.Provider;

import xapi.inject.X_Inject;
import xapi.reflect.api.ClassDataCallback;
import xapi.reflect.api.ReflectionService;

public class X_Reflect {

  private X_Reflect() {}
  
  //We have to put our mainly gwt-based reflection library in the main branch,
  //as shared code may need to be gwt-proofed.

  //As such, if you wish to use the reflection service without injection,
  //You must manually call GWT.create to get a JreReflectionService object
  //that you can use without this wrapper class
  public static final Provider<ReflectionService> singleton =
    X_Inject.singletonLazy(ReflectionService.class);

  public static <T> T[] newArray(Class<T> classLit, int length){
    return singleton.get().newArray(classLit, length);
  }

  /**
   * This is an unfortunate workaround for gwt-dev mode's isolated
   * classloader not returning packages.
   *
   * If you use super dev mode instead of dev mode, you'll never need this
   * method.  If you do use dev mode, you should route all Class.getPackage()
   * through this method, which uses the thread classloader to get a regular
   * jre class (one that will cause ClassCastException if used reflectively!).
   *
   * This method can be of limited use for production mode; it will return a
   * package for a class completely lacking in package based annotations.
   *
   * @param classLit
   * @return
   */
  public static Package getPackage(Class<?> classLit) {
    return singleton.get().getPackage(classLit);
  }

  //TODO return a latch/key of some kind, or Future<IsClass>
  public static <T> void async(final Class<T> classLit,
    final  ClassDataCallback<T> callback) {
    singleton.get().async(classLit, callback);
  }
  public static <T> void async(final String qualifiedSourceName,
    final  ClassDataCallback<T> callback) {
//    singleton.get().async(classLit, callback);
  }

  /**
   * In order to selectively enable full class support in gwt,
   * we need to be able to see the class literal in the generator.
   *
   * You MUST only send class literals to this method;
   * Class&lt;MyClass> cls = X_Reflect.magicClass(MyClass.class);
   *
   * Now you may use cls like any other class, to the extent provided by MagicClass,
   * and you may pass the variable around and use references to it in other X_Inject methods.
   * GWT.create() does not support magic classes, as we don't want to rewrite that method for anyone.
   * Use X_Inject.instance(), and it will default to GWT.create().
   * If you do use @InstanceOverride to inject a different class, it will also be instantiated w/ GWT.create().
   *
   * This method will call into a magic-method generator,
   * which pulls in all the required metadata at compile time,
   * and emits a "subclass" of java.lang.Class.
   *
   * This is completely illegal in java, but gwt is js, so anything goes.
   *
   * Rather than glob up a bunch of metadata into all class literals,
   * and cause terrible bloat across the app,
   * this method lets you select which classes you want full metadata on,
   * and which others will reduce to the typical, mostly-unsupported emulated Class.
   *
   * Note that for your gwt app to compile with references to the mostly unsupported class objects,
   * you must have xapi-super above gwt-user and gwt-dev on your classpath.
   * Our emulated class provides the method stubs, which throw exceptions to wrap objects w/ #magicClass.
   *
   *
   *
   * @param cls
   * @return
   */
  public static <T> Class<T> magicClass(Class<T> cls) {
    return singleton.get().magicClass(cls);
  }

}
