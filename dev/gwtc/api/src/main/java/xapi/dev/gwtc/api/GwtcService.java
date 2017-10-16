package xapi.dev.gwtc.api;

import xapi.fu.In1;
import xapi.fu.In2;
import xapi.fu.In2.In2Unsafe;
import xapi.fu.Out1;
import xapi.gwtc.api.CompiledDirectory;
import xapi.gwtc.api.DefaultValue;
import xapi.gwtc.api.GwtManifest;
import xapi.gwtc.api.GwtcProperties;
import xapi.gwtc.api.ServerRecompiler;

import java.lang.annotation.Annotation;
import java.net.URLClassLoader;
import java.util.concurrent.TimeUnit;

public interface GwtcService {

  default void addClassTo(String moduleName, Class<?> clazz) {
    getProject(moduleName, null).addClass(clazz);
  }


  GwtcProjectGenerator getProject(String moduleName, ClassLoader resources);

  String generateCompile(GwtManifest manifest);
  URLClassLoader resolveClasspath(GwtManifest manifest, String compileHome);
  In1<Integer> prepareCleanup(GwtManifest manifest);
  In2Unsafe<Integer, TimeUnit> startTask(Runnable task, URLClassLoader loader);
  int compile(GwtManifest manifest);
  GwtcJobState recompile(GwtManifest manifest, Long millisToWait, In2<ServerRecompiler, Throwable> callback);
  void compile(GwtManifest manifest, long timeout, TimeUnit unit, In2<Integer, Throwable> callback);
  void doCompile(GwtManifest manifest, long timeout, TimeUnit unit, In2<CompiledDirectory, Throwable> callback);

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
