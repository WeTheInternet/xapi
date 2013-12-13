package xapi.dev.gwtc.api;

import java.io.File;
import java.lang.reflect.Method;

import xapi.gwtc.api.GwtManifest;

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
  void addJUnitClass(Class<?> clazz);
  void addAsyncBlock(Class<? extends RunAsyncCallback> asSubclass);
  int compile(GwtManifest manifest);
  void addGwtModules(Class<?> clazz);
  String getModuleName();
  File getTempDir();

}
