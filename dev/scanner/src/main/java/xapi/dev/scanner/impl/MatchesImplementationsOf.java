package xapi.dev.scanner.impl;

import xapi.bytecode.ClassFile;
import xapi.collect.prefixed.PrefixedMap;
import xapi.fu.Filter.Filter1;
import xapi.log.X_Log;

import java.util.ArrayList;
import java.util.List;

public class MatchesImplementationsOf implements Filter1<ClassFile> {


  private final PrefixedMap<ByteCodeResource> bytecode;
  private final List<String> interfaces, classes;

  public MatchesImplementationsOf(PrefixedMap<ByteCodeResource> bytecode, String ... subclasses) {
    this.bytecode = bytecode;
    interfaces = new ArrayList<String>();
    classes = new ArrayList<String>();
    for (String subclass : subclasses) {
      ByteCodeResource resource = bytecode.get(subclass);
      if (resource == null) {
        X_Log.warn(getClass(), "Searching for implementations of",subclass,"that are not on search classpath");
        interfaces.add(subclass);
        classes.add(subclass);
      } else {
        ClassFile cls = bytecode.get(subclass).getClassData();
        (cls.isInterface() ? interfaces : classes).add(subclass);
      }
    }
  }

  @Override
  public Boolean io(ClassFile value) {
    for (String iface : value.getInterfaces()) {
      for (String subclass : interfaces) {
        if (iface.equals(subclass)) {
          return true;
        }
      }
    }
    if (!value.isInterface()) {
      for (String superclass : getHierarchy(value)) {
        for (String subclass : classes) {
          if (superclass.equals(subclass)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private Iterable<String> getHierarchy(ClassFile value) {
    List<String> hierarchy = new ArrayList<String>();
    addSuperclasses(hierarchy, value.getSuperclass(), value);
    return hierarchy;
  }

  private void addSuperclasses(List<String> hierarchy, String superclass, ClassFile clazz) {
    if (superclass != null) {
      hierarchy.add(superclass);
      if (superclass.equals("java.lang.Object")) {
        return;
      }
      ByteCodeResource resource = bytecode.get(superclass);
      if (resource == null) {
        X_Log.trace(getClass(), "Unable to find superclass",superclass,"of",clazz,"on classpath; supertype search will end");
      } else {
        ClassFile cls = resource.getClassData();
        addSuperclasses(hierarchy, cls.getSuperclass(), cls);
      }
    }
  }


}
