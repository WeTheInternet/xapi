package com.google.gwt.reflect.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.typeinfo.HasAnnotations;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ReflectionUtilType {

  public static Annotation[] extractAnnotations(final int annotationRetention, final HasAnnotations annoProvider) {
    final ArrayList<Annotation> annos = new ArrayList<Annotation>();
    final boolean keepClass = (annotationRetention | ReflectionStrategy.COMPILE) == annotationRetention;
    final boolean keepRuntime = (annotationRetention | ReflectionStrategy.RUNTIME) == annotationRetention;
    for (final Annotation anno : annoProvider.getDeclaredAnnotations()) {
      final Retention retention = anno.annotationType().getAnnotation(Retention.class);
      if (retention == null) {
        if (keepClass) {
          annos.add(anno);
        }
      } else {
        switch (retention.value()) {
          case CLASS:
            if (keepClass) {
              annos.add(anno);
            }
            break;
          case RUNTIME:
            if (keepRuntime) {
              annos.add(anno);
            }
            break;
          default:
        }
      }
    }
    return annos.toArray(new Annotation[annos.size()]);
  }

  public static JField findField(final TreeLogger logger,
      final JClassType type, final String name, final boolean declared) {
    for (final JField field : type.getFields()) {
      if (field.getName().equals(name)) {
        if (declared){
          if (!field.getEnclosingType().getQualifiedSourceName().equals(type.getQualifiedSourceName())) {
            logger.log(Type.TRACE,
                "Field with same name and different enclosing type skipped because declared-only field was requested;\n"
                    +field.getEnclosingType().getQualifiedSourceName()+" != "+type.getQualifiedSourceName());
            continue;
          }
        } else {
          if (!field.isPublic()) {
            logger.log(Type.TRACE,
                "Non-public field " +field.getEnclosingType().getName()+"."+ field.getName() + " skipped because declared=false.");
            continue;
          }
        }
          return field;
        }
    }
    return declared || type.getSuperclass() == null ? null: findField(logger, type.getSuperclass(), name, declared);
  }

  public static JMethod findMethod(final TreeLogger logger,
      final JClassType type, final String name, final List<String> params, final boolean declared) {
    loop:
    for (final JMethod method : type.getMethods()) {
      if (method.getName().equals(name)) {
        if (declared){
          if (!method.getEnclosingType().getQualifiedSourceName().equals(type.getQualifiedSourceName())) {
            logger.log(Type.TRACE,
                "Method with same name and different enclosing type skipped because declared-only method was requested;\n"
                +method.getEnclosingType().getQualifiedSourceName()+" != "+type.getQualifiedSourceName());
            continue;
          }
        } else {
          if (!method.isPublic()) {
            logger.log(Type.TRACE,
                "Non-public method " +method.getJsniSignature() + " skipped because declared=false.");
            continue;
          }
        }
        final JType[] types = method.getParameterTypes();
        if (types.length == params.size()) {
          for (int i = 0, m = types.length; i < m; i++ ) {
            String typeName = types[i].getErasedType().getQualifiedBinaryName();
            while (typeName.startsWith("[")) {
              typeName = typeName.substring(1)+"[]";
            }
            if (!params.get(i).equals(typeName)) {
              logger.log(Type.TRACE, "Method with same name and different signature; "
                  +name+"("+params+") mismatches at index "+i+" with value "+typeName);
              continue loop;
            }
          }
          return method;
        }
      }
    }
    return declared || type.getSuperclass() == null ? null: findMethod(logger, type.getSuperclass(), name, params, declared);
  }

  public static JConstructor findConstructor(final TreeLogger logger,
      final JClassType type, final List<String> params, final boolean declared) {
    loop:
      for (final JConstructor ctor : type.getConstructors()) {
          final JType[] types = ctor.getParameterTypes();
          if (types.length == params.size()) {
            for (int i = 0, m = types.length; i < m; i++ ) {
              String typeName = types[i].getErasedType().getQualifiedBinaryName();
              while (typeName.startsWith("[")) {
                typeName = typeName.substring(1)+"[]";
              }
              if (!params.get(i).equals(typeName)) {
                logger.log(Type.DEBUG, "constructor with different signature; "
                    +"("+params+") mismatches at index "+i+" with value "+typeName);
                continue loop;
              }
            }
            return ctor;
          }
        }
        return declared || type.getSuperclass() == null ? null: findConstructor(logger, type.getSuperclass(), params, declared);
  }

  public static int getModifiers(final JConstructor ctor) {
    int mod;

    if (ctor.isPublic()) {
      mod = Modifier.PUBLIC;
    } else if (ctor.isPrivate()) {
      mod = Modifier.PRIVATE;
    } else if (ctor.isProtected()) {
      mod = Modifier.PROTECTED;
    }
    else {
      mod = 0;//Package Protected
    }

    if (ctor.isVarArgs()) {
      mod |= 0x80;
    }

    return mod;
  }

  public static int getModifiers(final JField field) {
    int mod;

    if (field.isPublic()) {
      mod = Modifier.PUBLIC;
    } else if (field.isPrivate()) {
      mod = Modifier.PRIVATE;
    } else if (field.isProtected()) {
      mod = Modifier.PROTECTED;
    }
    else {
      mod = 0;//Package Protected
    }

    if (field.isFinal()) {
      mod |= Modifier.FINAL;
    }
    if (field.isStatic()) {
      mod |= Modifier.STATIC;
    }
    if (field.isTransient()) {
      mod |= Modifier.TRANSIENT;
    }
    if (field.isVolatile()) {
      mod |= Modifier.VOLATILE;
    }

    return mod;
  }

  public static int getModifiers(final JMethod method) {
    int mod;
    if (method.isPublic()) {
      mod = Modifier.PUBLIC;
    } else if (method.isPrivate()) {
      mod = Modifier.PRIVATE;
    } else if (method.isProtected()) {
      mod = Modifier.PROTECTED;
    }
    else {
      mod = 0;//Package Protected
    }

    if (method.isFinal()) {
      mod |= Modifier.FINAL;
    }
    if (method.isStatic()) {
      mod |= Modifier.STATIC;
    }
    if (method.isVarArgs())
     {
      mod |= 0x80;//Modifier.VARARGS;
    }
    if (method.isAbstract()) {
      mod |= Modifier.ABSTRACT;
    }
    if (method.isNative()) {
      mod |= Modifier.NATIVE;
    }
    if (method.isAnnotationMethod()!=null)
     {
      mod |= 0x2000;//Modifier.ANNOTATION;
    }

    return mod;
  }

  public static <T extends JType> String toJsniClassLits(final T[] types) {
    final StringBuilder b = new StringBuilder("[");
    for (int i = 0; i < types.length; i++) {
      b.append(toJsniClassLit(types[i]));
      if (i < types.length-1) {
        b.append(", ");
      }
    }
    return b + "]";
  }

  public static String toUniqueFactory(final JParameter params[], final JConstructor[] ctors) {
    final int length = params.length;
    final ArrayList<JConstructor> sameSize = new ArrayList<JConstructor>();
    for (final JConstructor ctor : ctors) {
      if (ctor.getParameters().length == length) {
        sameSize.add(ctor);
      }
    }
    if (sameSize.size() == 1) {
      return ReflectionUtilType.simplify(params);
    }
    final HashSet<String> unique = new HashSet<String>();
    for (final JConstructor method : sameSize) {
      final String simple = ReflectionUtilType.simplify(method.getParameters());
      unique.add(simple);
    }
    if (unique.size() == sameSize.size()) {
      return simpleParams(params);
    }
    return ReflectionUtilType.verboseParams(params);
  }

  public static String toUniqueFactory(final JParameter params[], final JMethod[] methods) {
    final int length = params.length;
    final ArrayList<JMethod> sameSize = new ArrayList<JMethod>();
    for (final JMethod method : methods) {
      if (method.getParameters().length == length) {
        sameSize.add(method);
      }
    }
    if (sameSize.size() == 1) {
      return ReflectionUtilType.simplify(params);
    }
    final HashSet<String> unique = new HashSet<String>();
    for (final com.google.gwt.core.ext.typeinfo.JMethod method : sameSize) {
      final String simple = ReflectionUtilType.simplify(method.getParameters());
      unique.add(simple);
    }
    if (unique.size() == sameSize.size()) {
      return simpleParams(params);
    }
    return ReflectionUtilType.verboseParams(params);
  }

  public static JClassType findType(final TypeOracle oracle, final Class<?> cls) {
    final String pkg = cls.getPackage().getName();
    return oracle.findType(pkg, cls.getCanonicalName().replace(pkg+".", ""));
  }

  private static String simpleParams(final com.google.gwt.core.ext.typeinfo.JParameter[] params) {
    final StringBuilder b = new StringBuilder();
    for (final com.google.gwt.core.ext.typeinfo.JParameter param : params) {
      b.append(param.getType().getErasedType().getQualifiedSourceName().charAt(0));
    }
    return b.toString();
  }

  private static String simplify(final com.google.gwt.core.ext.typeinfo.JParameter[] params) {
    final StringBuilder b = new StringBuilder();
    for (final com.google.gwt.core.ext.typeinfo.JParameter param : params) {
      b.append(param.getType().getErasedType().getQualifiedSourceName().charAt(0));
    }
    return b.toString();
  }

  private static String toJsniClassLit(final JType type) {
    return "@"+type.getErasedType().getQualifiedSourceName()+"::class";
  }

  private static String verboseParams(final com.google.gwt.core.ext.typeinfo.JParameter[] params) {
    final StringBuilder b = new StringBuilder();
    for (final com.google.gwt.core.ext.typeinfo.JParameter param : params) {
      String name = param.getType().getErasedType().getSimpleSourceName();
      name = name.replaceAll("\\[\\]", "");
      b.append(name).append('_');
    }
    return b.toString();
  }

  public static JPrimitiveType isPrimitiveWrapper(final JType type) {
    if (type.getQualifiedSourceName().startsWith("java.lang")) {
      if ("Integer".equals(type.getSimpleSourceName())) {
        return JPrimitiveType.INT;
      }
      if ("Boolean".equals(type.getSimpleSourceName())) {
        return JPrimitiveType.BOOLEAN;
      }
      if ("Double".equals(type.getSimpleSourceName())) {
        return JPrimitiveType.DOUBLE;
      }
      if ("Long".equals(type.getSimpleSourceName())) {
        return JPrimitiveType.LONG;
      }
      if ("Float".equals(type.getSimpleSourceName())) {
        return JPrimitiveType.FLOAT;
      }
      if ("Character".equals(type.getSimpleSourceName())) {
        return JPrimitiveType.CHAR;
      }
      if ("Byte".equals(type.getSimpleSourceName())) {
        return JPrimitiveType.BYTE;
      }
      if ("Short".equals(type.getSimpleSourceName())) {
        return JPrimitiveType.SHORT;
      }
    }
    return null;
  }

}
