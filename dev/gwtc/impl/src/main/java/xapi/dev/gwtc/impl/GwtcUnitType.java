package xapi.dev.gwtc.impl;

import java.lang.reflect.Method;

enum GwtcUnitType {
  Class, Package, Method;

  public static GwtcUnitType fromObject(Object source) {
    if (source instanceof Class) {
      return Class;
    }
    if (source instanceof Package) {
      return Package;
    }
    if (source instanceof Method) {
      return Method;
    }
    throw new IllegalArgumentException("Source object "+source+" is not a method, class or package");
  }
}