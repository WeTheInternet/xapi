package xapi.reflect.impl;

import java.lang.reflect.Array;
import java.net.URL;

import xapi.annotation.inject.SingletonDefault;
import xapi.reflect.service.ReflectionService;

@SingletonDefault(implFor=ReflectionService.class)
public class ReflectionServiceDefault implements ReflectionService{

  @Override
  @SuppressWarnings("unchecked")
  public <T> T[] newArray(Class<T> classLit, int dimension) {
    return (T[])Array.newInstance(classLit, dimension);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T[] newArray(Class<T> classLit, int ... dimensions) {
    return (T[])Array.newInstance(classLit, dimensions);
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
      URL loc = Thread.currentThread().getContextClassLoader().getResource(pkgInfo);
      if (loc != null) {
        try {
          cl.loadClass(parentName+".package-info");
          pkg = Package.getPackage(parentName);
        } catch (ClassNotFoundException ignored) {}
      }
    }
    return pkg;
  }

  @Override
  public Package getPackage(Object o) {
    return o.getClass().getPackage();
  }
  
}
