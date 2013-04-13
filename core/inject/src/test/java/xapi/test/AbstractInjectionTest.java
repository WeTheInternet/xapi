package xapi.test;

import java.net.URL;

import org.junit.Before;

import xapi.time.impl.RunOnce;
import xapi.util.X_Namespace;

public class AbstractInjectionTest {

  private static final RunOnce once = new RunOnce();

  @Before // can't do BeforeClass because we need to call this.getClass()
  public void prepareInjector(){
    // but, we only want to run injection once, so we use a simple lock
    if (once.shouldRun(false)) {
      System.setProperty(X_Namespace.PROPERTY_RUNTIME_META, "target/test-classes");
      URL location = getClass().getProtectionDomain().getCodeSource().getLocation();
      String testClasses = location.toExternalForm().split("wetheinter")[0];
      testClasses = testClasses.replace("file:", "");
      try {
        Object injector = getClass().getClassLoader()
          .loadClass("xapi.jre.inject.RuntimeInjector")
          .newInstance();

        injector.getClass().getMethod("writeMetaInfo", String.class, String.class, String.class)
          .invoke(injector,testClasses, "META-INF/singletons", "META-INF/instances");
      } catch (Exception e) {
        System.err.println("[WARN]: Runtime injection failure for "+getClass());
        e.printStackTrace();
      }
    }
  }
}
