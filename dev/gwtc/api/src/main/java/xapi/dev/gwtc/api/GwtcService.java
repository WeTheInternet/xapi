package xapi.dev.gwtc.api;

import xapi.dev.api.MavenLoader;
import xapi.fu.In1;
import xapi.fu.In2;
import xapi.fu.Mutable;
import xapi.fu.Out1;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.DefaultValue;
import xapi.gwtc.api.GwtManifest;
import xapi.gwtc.api.GwtcProperties;

import java.lang.annotation.Annotation;
import java.net.URLClassLoader;
import java.util.concurrent.TimeUnit;

public interface GwtcService {

  default void addClassTo(String moduleName, Class<?> clazz) {
    getProject(moduleName).addClass(clazz);
  }

  URLClassLoader ensureMeetsMinimumRequirements(URLClassLoader classpath);

  GwtcProjectGenerator getProject(String moduleName, ClassLoader resources);

  GwtcJobManager getJobManager();

  default GwtcProjectGenerator getProject(String moduleName) {
    return getProject(moduleName, Thread.currentThread().getContextClassLoader());
  }

  void generateCompile(GwtManifest manifest);
  URLClassLoader resolveClasspath(GwtManifest manifest);

  MavenLoader getMavenLoader();
  In1<Integer> prepareCleanup(GwtManifest manifest);

  default int compile(GwtManifest manifest) {
    final Mutable<Integer> result = new Mutable<>(-2);
    final long ttl = getMillisToWait();
    doCompile(manifest, ttl, TimeUnit.MILLISECONDS, (dir, error) -> {
      result.in(error == null && dir != null? 0 : -1);
      synchronized (result) {
        result.notifyAll();
      }
    });
    if (result.isNull()) {
      // we probably don't need to do this, as the semantics of doCompile are currently blocking,
      // we also don't want to have bugs should those semantics change (i.e. be configurable by subclassing)
      synchronized (result) {
        try {
          result.wait(ttl, 0);
        } catch (InterruptedException e) {
          return -1;
        }
      }
    }
    return result.out1();
  }

  default long getMillisToWait() {
    return TimeUnit.MINUTES.toMillis(3);
  }

  default void doCompile(GwtManifest manifest, long timeout, TimeUnit unit, In2<CompiledDirectory, Throwable> callback) {
    doCompile(false, manifest, timeout, unit, callback);
  }
  void doCompile(boolean avoidWork, GwtManifest manifest, long timeout, TimeUnit unit, In2<CompiledDirectory, Throwable> callback);

//  File getTempDir();
  String inGeneratedDirectory(GwtManifest manifest, String filename);

  default DefaultValue getDefaultValue(Class<?> param, Annotation[] annos) {
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

  String extractGwtVersion(String gwtHome);

  GwtcProperties getDefaultLaunchProperties();

  Out1<? extends Iterable<String>> resolveDependency(GwtManifest manifest, AnnotatedDependency dependency);

}
