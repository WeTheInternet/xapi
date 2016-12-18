package xapi.dev;

import xapi.dev.gwtc.api.GwtcService;
import xapi.gwtc.api.DefaultValue;
import xapi.gwtc.api.Gwtc;
import xapi.inject.X_Inject;
import xapi.log.X_Log;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class X_Gwtc {

  public static void compile(String entryPoint, Gwtc ... settings) {
    final GwtcService service = X_Inject.instance(GwtcService.class);
    try {
      service.addClass(Thread.currentThread().getContextClassLoader().loadClass(entryPoint));
    } catch (ClassNotFoundException e) {
      X_Log.error(X_Gwtc.class, "Could not find class",entryPoint,"from",Thread.currentThread().getContextClassLoader(), e);
    }
  }

  public static GwtcService getServiceForMethod(Method method) {
    final GwtcService service = X_Inject.instance(GwtcService.class);
    if (method != null) {
      service.addMethod(method);
    }
    return service;
  }

  public static GwtcService getServiceForClass(Class<?> clazz) {
    final GwtcService service = X_Inject.instance(GwtcService.class);
    if (clazz != null) {
      service.addClass(clazz);
    }
    return service;
  }

  public static GwtcService getServiceForPackage(Package pkg, boolean recursive) {
    final GwtcService service = X_Inject.instance(GwtcService.class);
    if (pkg != null) {
      service.addPackage(pkg, recursive);
    }
    return service;
  }

  public static DefaultValue getDefaultValue(Class<?> param, Annotation[] annos) {
    for (Annotation anno : annos) {
      if (anno.annotationType().equals(DefaultValue.class)) {
        return (DefaultValue) anno;
      }
    }
    if (param.isPrimitive()) {
      if (param == char.class) {
        return DefaultValue.Defaults.DEFAULT_CHAR;
      } else if (param == long.class) {
        return DefaultValue.Defaults.DEFAULT_LONG;
      } else if (param == float.class) {
        return DefaultValue.Defaults.DEFAULT_FLOAT;
      } else if (param == double.class) {
        return DefaultValue.Defaults.DEFAULT_DOUBLE;
      } else if (param == boolean.class) {
        return DefaultValue.Defaults.DEFAULT_BOOLEAN;
      } else {
        return DefaultValue.Defaults.DEFAULT_INT;
      }
    } else if (param == String.class) {
      return DefaultValue.Defaults.DEFAULT_STRING;
    } else if (param.isArray()){
      return DefaultValue.Defaults.DEFAULT_OBJECT;
    } else {
      DefaultValue value = param.getAnnotation(DefaultValue.class);
      if (value == null) {
        return DefaultValue.Defaults.DEFAULT_OBJECT;
      } else {
        return value;
      }
    }
  }

}
