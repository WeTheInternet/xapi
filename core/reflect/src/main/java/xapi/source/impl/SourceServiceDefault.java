package xapi.source.impl;

import xapi.annotation.inject.SingletonDefault;
import xapi.except.NotYetImplemented;
import xapi.source.api.IsClass;
import xapi.source.api.IsType;
import xapi.source.service.SourceService;

@SingletonDefault(implFor=SourceService.class)
public class SourceServiceDefault implements SourceService  {

  private final TypeMap types = new TypeMap();

  @Override
  public IsClass parseClass(byte[] bytecode) {
    throw new NotYetImplemented("You must inherit wetheinter.net:xapi-jre-reflect for bytecode parsing");
  }

  @Override
  public IsClass parseClass(String source) {
    throw new NotYetImplemented("Source code parser is not yet implemented");
  }

  @Override
  public IsType toType(Class<?> cls) {
    Class<?> enclosing = cls.getEnclosingClass();
    if (enclosing == null)
      return types.getType(getPackage(cls), cls.getSimpleName());
    return types.getType(toType(enclosing), cls.getSimpleName());
  }

  /**
   * Gwt-dev mode has to acquire the package from a different classloader than
   * the isolated app classloader, which strips the package object from classes.
   * @param cls - The class to get the package name of
   * @return - cls.getPackage().getName() -> Works for all platforms but gwt dev.
   */
  protected String getPackage(Class<?> cls) {
    return cls.getPackage().getName();
  }

  @Override
  public IsType toType(String pkg, String enclosedName) {
    int ind = enclosedName.indexOf('.');
    if (ind == -1)
      return types.getType(pkg, enclosedName);
    IsType type = types.getType(pkg, enclosedName.substring(0, ind));
    while (true) {
      // eat the previous type
      enclosedName = enclosedName.substring(ind+1);
      // find the next type
      ind = enclosedName.indexOf('.', ind);
      if (ind == -1) {
        return types.getType(type, enclosedName);
      }
      type = types.getType(type, enclosedName.substring(0, ind));
      return type;
    }
  }

  @Override
  public char classSeparator() {
    return '.';
  }

  @Override
  public char packageSeparator() {
    return '.';
  }

}
