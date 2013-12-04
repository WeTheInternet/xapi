package xapi.dev.scanner.impl;

import xapi.bytecode.ClassFile;
import xapi.util.api.MatchesValue;

public class MatchesDirectSubclasses implements MatchesValue<ClassFile>{

  private final String[] subclasses;

  public MatchesDirectSubclasses(String ... subclasses) {
    this.subclasses = subclasses;
  }
  
  @Override
  public boolean matches(ClassFile value) {
    if (value.isInterface()) {
      for (String iface : value.getInterfaces()) {
        for (String subclass : subclasses) {
          if (iface.equals(subclass)) {
            return true;
          }
        }
      }
    } else {
      for (String subclass : subclasses) {
        if (value.getSuperclass().equals(subclass)) {
          return true;
        }
      }
    }
    return false;
  }
  
}
