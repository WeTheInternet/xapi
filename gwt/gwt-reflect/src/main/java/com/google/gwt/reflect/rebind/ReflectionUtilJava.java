package com.google.gwt.reflect.rebind;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.reflect.client.GwtReflect;

public class ReflectionUtilJava {

  public static final String MAGIC_CLASS_SUFFIX = "_MC";
  
  public static String generatedMagicClassName(String simpleName) {
    return simpleName+MAGIC_CLASS_SUFFIX;
  }

  public static Method[] getMethods(Annotation anno) {
    ArrayList<Method> methods = new ArrayList<Method>();
    Class<? extends Annotation> cls = anno.annotationType();
    for (Method method : cls.getDeclaredMethods()) {
      if (Modifier.isPublic(method.getModifiers()) && method.getDeclaringClass() == cls) {
        methods.add(method);
      }
    }
    return methods.toArray(new Method[methods.size()]);
  }

  public static String sourceName(Object defaultValue) {
    StringBuilder b = new StringBuilder();
    if (defaultValue instanceof Enum<?>) {
      Enum<?> e = (Enum<?>)defaultValue;
      b.append(e.getDeclaringClass().getCanonicalName());
      b.append('.');
      b.append(e.name());
    } else if (defaultValue instanceof Class<?>) {
      b.append(((Class<?>)defaultValue).getCanonicalName()+".class");
    } else if (defaultValue instanceof Annotation) {
      b.append(ReflectionUtilJava.annotationToString((Annotation)defaultValue));
    } else if (defaultValue.getClass().isArray()) {
      Class<?> c = defaultValue.getClass().getComponentType();
      b.append("new ");
      b.append(c.getCanonicalName());
      b.append("[]{ ");
      int length = GwtReflect.arrayLength(defaultValue);
      if (length > 0) {
        b.append(sourceName(GwtReflect.arrayGet(defaultValue, 0)));
      }
      for (int i = 1; i < length; i++ ) {
        b.append(", ");
        b.append(sourceName(GwtReflect.arrayGet(defaultValue, i)));
      }
      b.append("}");
    } else if (defaultValue instanceof String) {
      b.append('"');
      b.append(GwtReflect.escape((String)defaultValue));
      b.append('"');
    } else {
      // a primitive
      b.append(defaultValue);
      if (defaultValue instanceof Long) {
        if (((Long)defaultValue).longValue() > Integer.MAX_VALUE) {
          b.append('L');
        }
      }
    }
    return b.toString();
  }

  public static String toSourceName(java.lang.reflect.Type type) {
    return toSourceName(type, true);
  }
  private static String toSourceName(java.lang.reflect.Type type, boolean keepGenericNames) {
    if (type instanceof Class) {
      return toSourceName((Class<?>)type, keepGenericNames);
    } else if (type instanceof TypeVariable) {
      return toSourceName((TypeVariable<?>)type, keepGenericNames);
    } else if (type instanceof ParameterizedType){
      return toSourceName((ParameterizedType)type);
    } else if (type instanceof WildcardType){
      return toSourceName((WildcardType)type);
    } else if (type instanceof GenericArrayType){
      return toSourceName((GenericArrayType)type);
    } else {
      System.err.println("Unknown type "+type+"; "+type.getClass().getName());
      throw new RuntimeException();
    }
  }

  private static String annotationToString(Annotation defaultValue) {
    if (GWT.isScript()) {
      return defaultValue.toString();
    } else {
      // dev mode has to dig for the serialized annotation.
      StringBuilder b = new StringBuilder("@");
      Class<? extends Annotation> cls = defaultValue.annotationType();
      b.append(cls.getCanonicalName());
      Method[] methods = cls.getMethods();
      if (methods.length > 0) {
        b.append('(');
        for (int i = 0, m = methods.length; i < m; i ++) {
          Method method = methods[i];
          if (method.getDeclaringClass().getName().equals("java.lang.Object"))
            continue;
          if (method.getName().equals("annotationType"))
            continue;
          if (i > 0)
            b.append(", ");
          b.append(method.getName());
          b.append('=');
          try {
            b.append(sourceName(method.invoke(defaultValue)));
          } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
          }
        }
        b.append(')');
      }
      return b.toString();
    }
  }

  private static String toSourceName(Class<?> returnType, boolean keepGenericTypeName) {
    StringBuilder b = new StringBuilder();
    int arrayDepth = 0;
    while (returnType.getComponentType() != null) {
      arrayDepth ++;
      returnType = returnType.getComponentType();
    }
    b.append(returnType.getCanonicalName());
    TypeVariable<?>[] params = returnType.getTypeParameters();
    if (params.length > 0) {
      b.append('<');
      b.append(toSourceName(params[0], keepGenericTypeName));
      for (int i = 1, m = params.length; i < m; i ++ ) {
        b.append(", ").append(toSourceName(params[i], keepGenericTypeName));
      }
      b.append("> ");
    }
    for (;arrayDepth-->0;) {
      b.append("[]");
    }
    return b.toString();
  }

  private static String toSourceName(GenericArrayType type) {
    return toSourceName(type.getGenericComponentType())+"[]";
  }
  private static String toSourceName(ParameterizedType returnType) {
    StringBuilder b = new StringBuilder();
    Class<?> c = (Class<?>)returnType.getRawType();
    int arrayDepth = 0;

    while (c.getComponentType() != null) {
      arrayDepth ++;
      c = c.getComponentType();
    }
    b.append(c.getCanonicalName());
    java.lang.reflect.Type[] params = returnType.getActualTypeArguments();
    if (params.length > 0) {
      b.append('<');
      b.append(toSourceName(params[0]));
      for (int i = 1, m = params.length; i < m; i ++ ) {
        b.append(", ").append(toSourceName(params[i]));
      }
      b.append("> ");
    }
    for(; arrayDepth --> 0; ) {
      b.append("[]");
    }
    return b.toString();
  }

  private static String toSourceName(TypeVariable<?> typeVariable, boolean keepGenericTypeName) {
    StringBuilder b = new StringBuilder();
    if (keepGenericTypeName) {
      b.append(typeVariable.getName());
    }
    java.lang.reflect.Type[] bounds = typeVariable.getBounds();
    if (bounds.length > 0) {
      if (keepGenericTypeName) {
        b.append(" extends ");
      }
      b.append(toSourceName(bounds[0]));
      for (int i = 1, m = bounds.length; i < m; i++) {
        b.append(", ").append(toSourceName(bounds[i]));
      }
    }
    return b.toString();
  }

  private static String toSourceName(WildcardType type) {
    StringBuilder b = new StringBuilder("?");
    java.lang.reflect.Type[] bounds;
    if (type.getUpperBounds().length > 0) {
      b.append(" extends ");
      bounds = type.getUpperBounds();
    } else if (type.getLowerBounds().length > 0){
      b.append(" super ");
      bounds = type.getLowerBounds();
    } else {
      return b.toString();
    }
    b.append(toSourceName(bounds[0], false));
    for (int i = 1, m = bounds.length; i < m; i ++) {
      b.append(" & ").append(toSourceName(bounds[i]));
    }
    return b.toString();
  }
  
  private ReflectionUtilJava() {}

}
