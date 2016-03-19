package xapi.reflect.impl;

import xapi.annotation.inject.SingletonDefault;
import xapi.reflect.service.ReflectionService;
import xapi.util.X_Util;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;

@SingletonDefault(implFor=ReflectionService.class)
public class ReflectionServiceDefault implements ReflectionService{

  @Override
  @SuppressWarnings("unchecked")
  public <T> T[] newArray(Class<T> classLit, int dimension) {
    return (T[])Array.newInstance(classLit, dimension);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T[][] newArray(Class<T> classLit, int dim1, int dim2) {
    return (T[][])Array.newInstance(classLit, dim1, dim2);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T[][][] newArray(Class<T> classLit, int dim1, int dim2, int dim3) {
    return (T[][][])Array.newInstance(classLit, dim1, dim2, dim3);
  }

  @Override
  public <T> Class<T> magicClass(Class<T> classLit) {
    return classLit;
  }

  @Override
  public Package getPackage(String parentName, ClassLoader cl) {
    Package pkg = Package.getPackage(parentName);
    if (pkg == null) {
      String pkgInfo = parentName.replace('.', '/')+"/package-info.class";
      URL loc = cl.getResource(pkgInfo);
      if (loc != null) {
        try {
          cl.loadClass(parentName+".package-info");
          pkg = Package.getPackage(parentName);
        } catch (ClassNotFoundException ignored) {}
      }
    }
    return pkg;
  }

  /**
   */
  public Object invokeDefaultMethod(final Method method, final Object ... params) {
    // create a crappy proxy of the declaring interface of this method (if it's default, it's an interface / annotation)
    // this proxy will fail to invoke any non-default methods.  If you need concrete methods, you need to use a real instance.
    final Object t =  java.lang.reflect.Proxy.newProxyInstance(getClassLoader(),
        new Class<?>[]{method.getDeclaringClass()},new java.lang.reflect.InvocationHandler() {
          @Override
          public Object invoke(final Object proxy, final Method method, final Object[] args)
          throws Throwable {
            return null;
          }
        });

    try {

      // Use reflection on some reflection classes to get access to a Lookup object we aren't supposed to touch :-)
      // TODO: add proper security wrapper code
      final Field field = java.lang.invoke.MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
      field.setAccessible(true);
      final java.lang.invoke.MethodHandles.Lookup lookup = (java.lang.invoke.MethodHandles.Lookup) field.get(null);
      final Object value = lookup
          .unreflectSpecial(method, method.getDeclaringClass())
          .bindTo(t)
          .invokeWithArguments();
      return value;
    } catch (Throwable e) {
      e.printStackTrace();
      throw X_Util.rethrow(e);
    }
  }

  protected ClassLoader getClassLoader() {
    return Thread.currentThread().getContextClassLoader();
  }

  @Override
  public Package getPackage(Object o) {
    return o.getClass().getPackage();
  }

}
