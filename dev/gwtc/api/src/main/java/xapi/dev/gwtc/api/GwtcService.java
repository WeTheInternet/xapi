package xapi.dev.gwtc.api;

import xapi.dev.source.MethodBuffer;
import xapi.dev.source.SourceBuilder;
import xapi.dev.source.SourceBuilder.JavaType;
import xapi.fu.In1;
import xapi.fu.In2;
import xapi.fu.In2Out1;
import xapi.fu.Out1;
import xapi.gwtc.api.GwtManifest;
import xapi.gwtc.api.ServerRecompiler;

import java.io.File;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.junit.tools.GWTTestSuite;

public interface GwtcService {

  void addMethod(Method method);
  void addMethod(Method method, boolean onNewInstance);
  void addClass(Class<?> clazz);
  void addPackage(Package pkg, boolean recursive);
  void addGwtTestCase(Class<? extends GWTTestCase> subclass);
  void addGwtTestSuite(Class<? extends GWTTestSuite> asSubclass);

  boolean addJUnitClass(Class<?> clazz);
  void addAsyncBlock(Class<? extends RunAsyncCallback> asSubclass);
  void addGwtModules(Class<?> clazz);
  void addClasspath(Class<?> cls);
  MethodBuffer addMethodToEntryPoint(String methodDef);
  void addGwtInherit(String inherit);

  int compile(GwtManifest manifest);
  In2Out1<Integer, TimeUnit, Integer> recompile(GwtManifest manifest, In2<ServerRecompiler, Throwable> callback);
  void compile(GwtManifest manifest, int timeout, TimeUnit unit, In1<Integer> callback);

  String getModuleName();
  SourceBuilder createJavaFile(String pkg, String filename, JavaType type);
  void createFile(String pkg, String filename, Out1<String> sourceProvider);
  File getTempDir();
  String inGeneratedDirectory(GwtManifest manifest, String filename);
  String modifyPackage(String pkgToUse);
  MethodBuffer getOnModuleLoad();

  String getSuggestedRoot();
}
