package xapi.dev;

import xapi.dev.gwtc.api.GwtcProjectGenerator;
import xapi.dev.gwtc.api.GwtcService;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.Gwtc;
import xapi.inject.X_Inject;
import xapi.log.X_Log;
import xapi.debug.X_Debug;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

public class X_Gwtc {

  public static CompiledDirectory compile(String entryPoint, Gwtc ... settings) {
    final GwtcService service = X_Inject.instance(GwtcService.class);
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    CompiledDirectory[] result = {null};
    Throwable[] error = {null};
    try {
      final GwtcProjectGenerator project = service.getProject(entryPoint + "Gen");
      project.addClass(cl.loadClass(entryPoint));
      service.doCompile(project.getManifest(), 5, TimeUnit.MINUTES, (res, fail)->{
        result[0] = res;
        error[0] = fail;
      });
    } catch (ClassNotFoundException e) {
      X_Log.error(X_Gwtc.class, "Could not find class",entryPoint,"from", cl, e);
    }
    if (error[0] != null) {
      throw X_Debug.rethrow(error[0]);
    }
    return result[0];
  }

  public static GwtcService getGeneratorForClass(Class<?> clazz, String moduleName) {
    final GwtcService service = X_Inject.instance(GwtcService.class);

    final GwtcProjectGenerator generator = service.getProject(moduleName);
    if (clazz != null) {
      generator.addClass(clazz);
    }
    return service;
  }

  public static GwtcService getGeneratorForMethod(Method method, String moduleName) {
    final GwtcService service = X_Inject.instance(GwtcService.class);

    final GwtcProjectGenerator generator = service.getProject(moduleName);
    if (method != null) {
      generator.addMethod(method);
    }
    return service;
  }

  public static GwtcService getGeneratorForPackage(Package pkg, String moduleName, boolean recursive) {
    final GwtcService service = X_Inject.instance(GwtcService.class);
    final GwtcProjectGenerator generator = service.getProject(moduleName);
    if (pkg != null) {
      generator.addPackage(pkg, recursive);
    }
    return service;
  }

}
