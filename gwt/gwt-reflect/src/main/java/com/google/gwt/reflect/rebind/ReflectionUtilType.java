package com.google.gwt.reflect.rebind;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.google.gwt.core.ext.typeinfo.HasAnnotations;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;

public class ReflectionUtilType {

  public static Annotation[] extractAnnotations(int annotationRetention,
      HasAnnotations method) {
    ArrayList<Annotation> annos = new ArrayList<Annotation>();
    boolean keepClass = (annotationRetention | ReflectionStrategy.COMPILE) == annotationRetention;
    boolean keepRuntime = (annotationRetention | ReflectionStrategy.RUNTIME) == annotationRetention;
    for (Annotation anno : method.getAnnotations()) {
      Retention retention = anno.annotationType().getAnnotation(Retention.class);
      if (retention == null) {
        if (keepClass)
          annos.add(anno);
      } else {
        switch (retention.value()) {
          case CLASS:
            if (keepClass)
              annos.add(anno);
            break;
          case RUNTIME:
            if (keepRuntime)
              annos.add(anno);
          default:
        }
      }
    }
    return annos.toArray(new Annotation[annos.size()]);
  }

  public static JMethod findMethod(
      JClassType type, String name, List<String> params, boolean declared) {
    loop:
    for (JMethod method : type.getMethods()) {
      if (method.getName().equals(name)) {
        if (declared && !method.getEnclosingType().getQualifiedSourceName().equals(type.getQualifiedSourceName()))
          continue;
        JType[] types = method.getParameterTypes();
        if (types.length == params.size()) {
          for (int i = 0, m = types.length; i < m; i++ ) {
            if (!params.get(i).equals(types[i].getErasedType().getQualifiedBinaryName())) {
              continue loop;
            }
          }
          return method;
        }
      }
    }
    return declared || type.getSuperclass() == null ? null: findMethod(type.getSuperclass(), name, params, declared);
  }

  public static int getModifiers(JConstructor ctor) {
    int mod;
    
    if (ctor.isPublic())
      mod = Modifier.PUBLIC;
    else if (ctor.isPrivate())
      mod = Modifier.PRIVATE;
    else if (ctor.isProtected())
      mod = Modifier.PROTECTED;
    else
      mod = 0;//Package Protected
    
    if (ctor.isVarArgs())
      mod |= 0x80;
    
    return mod;
  }
  
  public static int getModifiers(JField field) {
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

  public static int getModifiers(JMethod method) {
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

  public static <T extends JType> String toJsniClassLits(T[] types) {
    StringBuilder b = new StringBuilder("[");
    for (int i = 0; i < types.length; i++) {
      b.append(toJsniClassLit(types[i]));
      if (i < types.length-1)
        b.append(", ");
    }
    return b + "]";
  }

  public static String toUniqueFactory(JParameter params[], JConstructor[] ctors) {
    int length = params.length;
    ArrayList<JConstructor> sameSize = new ArrayList<JConstructor>();
    for (JConstructor ctor : ctors) {
      if (ctor.getParameters().length == length)
        sameSize.add(ctor);
    }
    if (sameSize.size() == 1) {
      return ReflectionUtilType.simplify(params);
    }
    HashSet<String> unique = new HashSet<String>();
    for (JConstructor method : sameSize) {
      String simple = ReflectionUtilType.simplify(method.getParameters());
      unique.add(simple);
    }
    if (unique.size() == sameSize.size()) {
      return simpleParams(params);
    }
    return ReflectionUtilType.verboseParams(params);
  }
  
  public static String toUniqueFactory(JParameter params[], JMethod[] methods) {
    int length = params.length;
    ArrayList<JMethod> sameSize = new ArrayList<JMethod>();
    for (JMethod method : methods) {
      if (method.getParameters().length == length)
        sameSize.add(method);
    }
    if (sameSize.size() == 1) {
      return ReflectionUtilType.simplify(params);
    }
    HashSet<String> unique = new HashSet<String>();
    for (com.google.gwt.core.ext.typeinfo.JMethod method : sameSize) {
      String simple = ReflectionUtilType.simplify(method.getParameters());
      unique.add(simple);
    }
    if (unique.size() == sameSize.size()) {
      return simpleParams(params);
    }
    return ReflectionUtilType.verboseParams(params);
  }

  private static String simpleParams(com.google.gwt.core.ext.typeinfo.JParameter[] params) {
    StringBuilder b = new StringBuilder();
    for (com.google.gwt.core.ext.typeinfo.JParameter param : params) {
      b.append(param.getType().getErasedType().getQualifiedSourceName().charAt(0));
    }
    return b.toString();
  }

  private static String simplify(com.google.gwt.core.ext.typeinfo.JParameter[] params) {
    StringBuilder b = new StringBuilder();
    for (com.google.gwt.core.ext.typeinfo.JParameter param : params) {
      b.append(param.getType().getErasedType().getQualifiedSourceName().charAt(0));
    }
    return b.toString();
  }

  private static String toJsniClassLit(JType type) {
    return "@"+type.getErasedType().getQualifiedSourceName()+"::class";
  }

  private static String verboseParams(com.google.gwt.core.ext.typeinfo.JParameter[] params) {
    StringBuilder b = new StringBuilder();
    for (com.google.gwt.core.ext.typeinfo.JParameter param : params) {
      String name = param.getType().getErasedType().getSimpleSourceName();
      name = name.replaceAll("\\[\\]", "");
      b.append(name).append('_');
    }
    return b.toString();
  }

  public static JPrimitiveType isPrimitiveWrapper(JType type) {
    if (type.getQualifiedSourceName().startsWith("java.lang")) {
      if ("Integer".equals(type.getSimpleSourceName()))
        return JPrimitiveType.INT;
      if ("Boolean".equals(type.getSimpleSourceName()))
        return JPrimitiveType.BOOLEAN;
      if ("Double".equals(type.getSimpleSourceName()))
        return JPrimitiveType.DOUBLE;
      if ("Long".equals(type.getSimpleSourceName()))
        return JPrimitiveType.LONG;
      if ("Float".equals(type.getSimpleSourceName()))
        return JPrimitiveType.FLOAT;
      if ("Character".equals(type.getSimpleSourceName()))
        return JPrimitiveType.CHAR;
      if ("Byte".equals(type.getSimpleSourceName()))
        return JPrimitiveType.BYTE;
      if ("Short".equals(type.getSimpleSourceName()))
        return JPrimitiveType.SHORT;
    }
    return null;
  }

}
