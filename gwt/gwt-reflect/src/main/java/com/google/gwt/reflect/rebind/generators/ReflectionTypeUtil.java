package com.google.gwt.reflect.rebind.generators;

import java.lang.reflect.Modifier;

public class ReflectionTypeUtil {

  static int getModifiers(com.google.gwt.core.ext.typeinfo.JField field) {
    int mod;
  
    if (field.isPublic())
      mod = Modifier.PUBLIC;
    else if (field.isPrivate())
      mod = Modifier.PRIVATE;
    else if (field.isProtected())
      mod = Modifier.PROTECTED;
    else
      mod = 0;//Package Protected
  
    if (field.isFinal())
      mod |= Modifier.FINAL;
    if (field.isStatic())
      mod |= Modifier.STATIC;
    if (field.isTransient())
      mod |= Modifier.TRANSIENT;
    if (field.isVolatile())
      mod |= Modifier.VOLATILE;
  
    return mod;
  }

  static int getModifiers(com.google.gwt.core.ext.typeinfo.JMethod method) {
    int mod;
    if (method.isPublic())
      mod = Modifier.PUBLIC;
    else if (method.isPrivate())
      mod = Modifier.PRIVATE;
    else if (method.isProtected())
      mod = Modifier.PROTECTED;
    else
      mod = 0;//Package Protected
  
    if (method.isFinal())
      mod |= Modifier.FINAL;
    if (method.isStatic())
      mod |= Modifier.STATIC;
    if (method.isVarArgs())
      mod |= 0x80;//Modifier.VARARGS;
    if (method.isAbstract())
      mod |= Modifier.ABSTRACT;
    if (method.isNative())
      mod |= Modifier.NATIVE;
    if (method.isAnnotationMethod()!=null)
      mod |= 0x2000;//Modifier.ANNOTATION;
  
    return mod;
  }

  static String toJsniClassLit(com.google.gwt.core.ext.typeinfo.JType type) {
    return "@"+type.getErasedType().getQualifiedSourceName()+"::class";
  }

  public static <T extends com.google.gwt.core.ext.typeinfo.JType> String toJsniClassLits(T[] types) {
    StringBuilder b = new StringBuilder("[");
    for (int i = 0; i < types.length; i++) {
      b.append(toJsniClassLit(types[i]));
      if (i < types.length-1)
        b.append(", ");
    }
    return b + "]";
  }

}
