package xapi.dev.reflect;

import java.lang.reflect.Modifier;

import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JType;

public class ReflectionGeneratorUtil {

  static int getModifiers(JField field) {
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

  static int getModifiers(JMethod method) {
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

  static String toJsniClassLit(JType type) {
    return "@"+type.getQualifiedSourceName()+"::class";
  }
  static <Type extends JType> String toJsniClassLits(Type[] types) {
    StringBuilder b = new StringBuilder("[");
    for (int i = 0; i < types.length; i++) {
      b.append(toJsniClassLit(types[i]));
      if (i < types.length-1)
        b.append(", ");
    }
    return b + "]";
  }


}
