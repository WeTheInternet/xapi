package xapi.reflect.impl;

import java.lang.reflect.Array;

import javax.validation.constraints.NotNull;

import xapi.annotation.inject.SingletonDefault;
import xapi.platform.GwtDevPlatform;
import xapi.reflect.service.ReflectionService;

@GwtDevPlatform
@SingletonDefault(implFor=ReflectionService.class)
public class GwtDevReflectionService implements ReflectionService{

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
    //TODO(maybe): lookup this class in our injector implementation,
    //And return the rebound target instead
      return classLit;
  }

  @Override
  public Package getPackage(@NotNull Object o) {
    //gwt-dev classloader doesn't load packages correctly...
    //so, we need to circumvent the IsolatedAppClassloader.
    //this will lead to some overhead, and could cause ClassCastException
    //if the user is unwary and directly accesses class.getPackage()
    //TODO fix IsolatedAppClassloader to keep packages.
    try {
      return Thread.currentThread().getContextClassLoader().loadClass(o.getClass().getName()).getPackage();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

}
