package xapi.dev.scanner.impl;

import xapi.bytecode.ClassFile;
import xapi.fu.Filter.Filter1;

public class MatchesDirectSubclasses implements Filter1<ClassFile> {

  private final String[] subclasses;

  public MatchesDirectSubclasses(String ... subclasses) {
    this.subclasses = subclasses;
  }

  @Override
  public boolean filter1(ClassFile value) {
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
