package com.google.gwt.reflect.shared;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;

public final class GwtReflectJre {

  public static Package getPackage(final String name) {
    return getPackage(name, Thread.currentThread().getContextClassLoader());
  }

  public static Package getPackage(final String name, final ClassLoader cl) {
    Package pkg = Package.getPackage(name);
    if (pkg == null) {
      final String pkgInfo = name.replace('.', '/')+"/package-info.class";
      final URL loc = Thread.currentThread().getContextClassLoader().getResource(pkgInfo);
      if (loc != null) {
        try {
          cl.loadClass(name+".package-info");
          pkg = Package.getPackage(name);
        } catch (final ClassNotFoundException ignored) {}
      }
    }
    return pkg;
  }

  /**
   * @param method
   * @param params
   * @return
   * @throws Throwable
   */
  public static Object invokeDefaultMethod(final Method method, final Object[] params) throws Throwable {
    final Object t=  java.lang.reflect.Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
        new Class<?>[]{method.getDeclaringClass()},new java.lang.reflect.InvocationHandler() {
      @Override
      public Object invoke(final Object proxy, final Method method, final Object[] args)
          throws Throwable {
        return null;
      }
    });

    final Field field = java.lang.invoke.MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
    field.setAccessible(true);
    final java.lang.invoke.MethodHandles.Lookup lookup = (java.lang.invoke.MethodHandles.Lookup) field.get(null);
    final Object value = lookup
        .unreflectSpecial(method, method.getDeclaringClass())
        .bindTo(t)
        .invokeWithArguments();
    return value;
  }

  @SuppressWarnings("unchecked")
  public static <T> T[] newArray(final Class<T> classLit, final int size) {
    return (T[])Array.newInstance(classLit, size);
  }


  @SuppressWarnings("unchecked")
  public static <T> T[][] newArray(final Class<T> classLit, final int dim1, final int dim2) {
    return (T[][])Array.newInstance(classLit, dim1, dim2);
  }

  private GwtReflectJre() {}
}
