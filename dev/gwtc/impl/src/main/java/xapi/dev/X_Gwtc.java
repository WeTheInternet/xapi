package xapi.dev;

import xapi.dev.gwtc.api.GwtcProjectGenerator;
import xapi.dev.gwtc.api.GwtcService;
import xapi.gwtc.api.DefaultValue;
import xapi.gwtc.api.Gwtc;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.model.api.PrimitiveSerializer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class X_Gwtc {

  public static void compile(String entryPoint, Gwtc ... settings) {
    final GwtcService service = X_Inject.instance(GwtcService.class);
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    try {
      final GwtcProjectGenerator project = service.getProject(entryPoint + "Gen", cl);
      project.addClass(cl.loadClass(entryPoint));

    } catch (ClassNotFoundException e) {
      X_Log.error(X_Gwtc.class, "Could not find class",entryPoint,"from", cl, e);
    }
  }

  public static GwtcService getGeneratorForClass(Class<?> clazz, String moduleName) {
    final GwtcService service = X_Inject.instance(GwtcService.class);
    // TODO extract classloader for clazz?

    final GwtcProjectGenerator generator = service.getProject(
        moduleName,
        Thread.currentThread().getContextClassLoader()
    );
    if (clazz != null) {
      generator.addClass(clazz);
    }
    return service;
  }

  public static GwtcService getGeneratorForMethod(Method method, String moduleName) {
    final GwtcService service = X_Inject.instance(GwtcService.class);
    // TODO extract classloader for clazz?

    final GwtcProjectGenerator generator = service.getProject(
        moduleName,
        Thread.currentThread().getContextClassLoader()
    );
    if (method != null) {
      generator.addMethod(method);
    }
    return service;
  }

  public static GwtcService getGeneratorForPackage(Package pkg, String moduleName, boolean recursive) {
    final GwtcService service = X_Inject.instance(GwtcService.class);
    // TODO extract classloader for clazz?

    final GwtcProjectGenerator generator = service.getProject(
        moduleName,
        Thread.currentThread().getContextClassLoader()
    );
    if (pkg != null) {
      generator.addPackage(pkg, recursive);
    }
    return service;
  }

}
