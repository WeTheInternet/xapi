package com.google.gwt.reflect.rebind;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JType;
import com.google.gwt.reflect.shared.GwtReflect;
import com.google.gwt.thirdparty.xapi.dev.source.MemberBuffer;
import com.google.gwt.thirdparty.xapi.source.read.JavaModel.IsQualified;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class ReflectionUtilJava  {

  public static final String MAGIC_CLASS_SUFFIX = "_MC";
  private static final Pattern DOUBLE_EXTENDS = Pattern.compile("extends [a-zA-Z0-9\\$_]+ extends");

  public static String generatedMagicClassName(final String simpleName) {
    return simpleName+MAGIC_CLASS_SUFFIX;
  }

  public static Method[] getMethods(final Annotation anno) {
    final ArrayList<Method> methods = new ArrayList<Method>();
    final Class<? extends Annotation> cls = anno.annotationType();
    for (final Method method : cls.getMethods()) {
      if (Modifier.isPublic(method.getModifiers()) && method.getDeclaringClass() == cls) {
        methods.add(method);
      }
    }
    return methods.toArray(new Method[methods.size()]);
  }

  public static String sourceName(final Object defaultValue) {
    final StringBuilder b = new StringBuilder();
    if (defaultValue instanceof Enum<?>) {
      final Enum<?> e = (Enum<?>)defaultValue;
      b.append(e.getDeclaringClass().getCanonicalName());
      b.append('.');
      b.append(e.name());
    } else if (defaultValue instanceof Class<?>) {
      b.append(((Class<?>)defaultValue).getCanonicalName()+".class");
    } else if (defaultValue instanceof Annotation) {
      b.append(ReflectionUtilJava.annotationToString((Annotation)defaultValue));
    } else if (defaultValue.getClass().isArray()) {
      final Class<?> c = defaultValue.getClass().getComponentType();
      b.append("new ");
      b.append(c.getCanonicalName());
      b.append("[]{ ");
      final int length = GwtReflect.arrayLength(defaultValue);
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

  public static String toSourceName(final Type type) {
    return toSourceName(type, true);
  }

  public static String toSourceName(final Type type, final MemberBuffer<?> buffer) {
    if (type instanceof Class) {
      return toSourceName((Class<?>)type, buffer);
    } else if (type instanceof TypeVariable) {
      return toSourceName((TypeVariable<?>)type, buffer);
    } else if (type instanceof ParameterizedType){
      return toSourceName((ParameterizedType)type, buffer);
    } else if (type instanceof WildcardType){
      return toSourceName((WildcardType)type, buffer);
    } else if (type instanceof GenericArrayType){
      return toSourceName((GenericArrayType)type, buffer);
    } else {
      System.err.println("Unknown type "+type+"; "+type.getClass().getName());
      throw new RuntimeException();
    }
  }

  private static String toSourceName(final Type type, final boolean keepGenericNames) {
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

  private static String annotationToString(final Annotation defaultValue) {
    if (GWT.isScript()) {
      return defaultValue.toString();
    } else {
      // dev mode has to dig for the serialized annotation.
      final StringBuilder b = new StringBuilder("@");
      final Class<? extends Annotation> cls = defaultValue.annotationType();
      b.append(cls.getCanonicalName());
      final Method[] methods = cls.getMethods();
      if (methods.length > 0) {
        b.append('(');
        for (int i = 0, m = methods.length; i < m; i ++) {
          final Method method = methods[i];
          if (method.getDeclaringClass().getName().equals("java.lang.Object")) {
            continue;
          }
          if (method.getName().equals("annotationType")) {
            continue;
          }
          if (i > 0) {
            b.append(", ");
          }
          b.append(method.getName());
          b.append('=');
          try {
            b.append(sourceName(method.invoke(defaultValue)));
          } catch (final Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
          }
        }
        b.append(')');
      }
      return b.toString();
    }
  }

  private static String toSourceName(Class<?> returnType, final boolean keepGenericTypeName) {
    final StringBuilder b = new StringBuilder();
    int arrayDepth = 0;
    while (returnType.getComponentType() != null) {
      arrayDepth ++;
      returnType = returnType.getComponentType();
    }
    b.append(returnType.getCanonicalName());
    final TypeVariable<?>[] params = returnType.getTypeParameters();
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

  private static String toSourceName(final GenericArrayType type) {
    return toSourceName(type.getGenericComponentType())+"[]";
  }
  private static String toSourceName(final ParameterizedType returnType) {
    final StringBuilder b = new StringBuilder();
    Class<?> c = (Class<?>)returnType.getRawType();
    int arrayDepth = 0;

    while (c.getComponentType() != null) {
      arrayDepth ++;
      c = c.getComponentType();
    }
    b.append(c.getCanonicalName());
    final java.lang.reflect.Type[] params = returnType.getActualTypeArguments();
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

  private static String toSourceName(final TypeVariable<?> typeVariable, final boolean keepGenericTypeName) {
    final StringBuilder b = new StringBuilder();
    if (keepGenericTypeName) {
      b.append(typeVariable.getName());
    }
    final java.lang.reflect.Type[] bounds = typeVariable.getBounds();
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

  private static String toSourceName(final WildcardType type) {
    final StringBuilder b = new StringBuilder("?");
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

  private static String toSourceName(Class<?> returnType, final MemberBuffer<?> buffer) {
    final StringBuilder b = new StringBuilder();
    int arrayDepth = 0;
    while (returnType.getComponentType() != null) {
      arrayDepth ++;
      returnType = returnType.getComponentType();
    }
    b.append(buffer.addImport(returnType));
    final TypeVariable<?>[] params = returnType.getTypeParameters();
    if (params.length > 0) {
      b.append('<');
      b.append(toSourceName(params[0], buffer));
      for (int i = 1, m = params.length; i < m; i ++ ) {
        b.append(", ").append(toSourceName(params[i], buffer));
      }
      b.append("> ");
    }
    for (;arrayDepth-->0;) {
      b.append("[]");
    }
    return b.toString();
  }

  private static String toSourceName(final GenericArrayType type, final MemberBuffer<?> buffer) {
    return toSourceName(type.getGenericComponentType(), buffer)+"[]";
  }
  private static String toSourceName(final ParameterizedType returnType, final MemberBuffer<?> buffer) {
    final StringBuilder b = new StringBuilder();
    Class<?> c = (Class<?>)returnType.getRawType();
    int arrayDepth = 0;

    while (c.getComponentType() != null) {
      arrayDepth ++;
      c = c.getComponentType();
    }
    b.append(buffer.addImport(c));
    final Type[] params = returnType.getActualTypeArguments();
    if (params.length > 0) {
      b.append('<');
      b.append(toSourceName(params[0], buffer));
      for (int i = 1, m = params.length; i < m; i ++ ) {
        b.append(", ").append(toSourceName(params[i], buffer));
      }
      b.append("> ");
    }
    for(; arrayDepth --> 0; ) {
      b.append("[]");
    }
    return b.toString();
  }

  private static String toSourceName(final TypeVariable<?> typeVariable, final MemberBuffer<?> buffer) {
    final StringBuilder b = new StringBuilder();
    final String name = typeVariable.getName();
    final Type[] bounds = typeVariable.getBounds();
    if (bounds.length == 0) {
      b.append(name);
    } else {
      final String type = toSourceName(bounds[0], buffer);
      if (!type.contains("extends")) {
        b.append(name);
        b.append(" extends ");
      }
      b.append(type);
      for (int i = 1, m = bounds.length; i < m; i++) {
        b.append(", ").append(toSourceName(bounds[i], buffer));
      }
    }
    return b.toString();
  }

  private static String toSourceName(final WildcardType type, final MemberBuffer<?> buffer) {
    final StringBuilder b = new StringBuilder("?");
    Type[] bounds;
    if (type.getUpperBounds().length > 0) {
      b.append(" extends ");
      bounds = type.getUpperBounds();
    } else if (type.getLowerBounds().length > 0){
      b.append(" super ");
      bounds = type.getLowerBounds();
    } else {
      return b.toString();
    }
    final StringBuilder subtype = new StringBuilder(toSourceName(bounds[0], buffer));
    for (int i = 1, m = bounds.length; i < m; i ++) {
      subtype.append(" & ").append(toSourceName(bounds[i], buffer));
    }
    b.append(subtype.toString().replaceAll("^[a-zA-Z0-9\\$_] extends ", ""));
    return b.toString();
  }

  private ReflectionUtilJava() {}

  public static String toFlatSimpleName(final JType classLit) {
    final StringBuilder b = new StringBuilder();
    final String[] compound = classLit.getCompoundName();
    for (int i = 0; i < compound.length; i++) {
      if (i > 0) {
        b.append('_');
      }
      b.append(compound[i]);
    }
    return b.toString();
  }

  public static String toFlatName(final String pkg) {
    return pkg.replace('.', '_');
  }

  public static IsQualified generatedAnnotationProviderName(
      final JClassLiteral classLit, final JClassLiteral annoLit) {
    final JType refType = classLit.getRefType();
    return new IsQualified(refType.getPackageName(),
        toFlatSimpleName(refType)+"__"+toFlatName(annoLit.getRefType().getName()));
  }

  /**
   * @param pkg -> The package name, possibly empty
   * @param name -> The type name, never empty.
   * @return pkg+"."+name, unless pkg is empty, in which case we return proxyName
   */
  public static String qualifiedName(final String pkg, final String name) {
    return (pkg.length()==0 ? "" : pkg + ".")+ name;

  }

  static String toUniqueName(final Class<?> cls) {
    return cls.getCanonicalName().replace('.', '_');
  }

}
