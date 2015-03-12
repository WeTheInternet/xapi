package com.google.gwt.reflect.shared;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;

import com.google.gwt.core.shared.GwtIncompatible;

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

  public static Object invokeDefaultMethod(final Method method, final Object[] params) throws Throwable {
    throw new UnsupportedOperationException("Cannot invoke default methods in GWT using a null instance");
  }
  
  public static <T> T[] newArray(final Class<?> classLit, final int size) {
    throw new UnsupportedOperationException("Cannot dynamically create arrays in GWT");
  }

  public static <T> T[][] newArray(final Class<T> classLit, final int dim1, final int dim2) {
    throw new UnsupportedOperationException("Cannot dynamically create arrays in GWT");
  }

  private GwtReflectJre() {}
}
