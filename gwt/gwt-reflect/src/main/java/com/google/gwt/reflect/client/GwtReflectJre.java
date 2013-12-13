package com.google.gwt.reflect.client;

import java.net.URL;

public class GwtReflectJre {

  public static Package getPackage(String name, ClassLoader cl) {
    Package pkg = Package.getPackage(name);
    if (pkg == null) {
      String pkgInfo = name.replace('.', '/')+"/package-info.class";
      URL loc = Thread.currentThread().getContextClassLoader().getResource(pkgInfo);
      if (loc != null) {
        try {
          cl.loadClass(name+".package-info");
          pkg = Package.getPackage(name);
        } catch (ClassNotFoundException ignored) {}
      }
    }
    return pkg;
  }

  public static Package getPackage(String name) {
    return getPackage(name, Thread.currentThread().getContextClassLoader());
  }
}
