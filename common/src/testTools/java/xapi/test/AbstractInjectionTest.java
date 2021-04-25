package xapi.test;

import org.junit.Before;
import org.junit.BeforeClass;
import xapi.inject.api.PlatformChecker;
import xapi.time.impl.RunOnce;
import xapi.constants.X_Namespace;

import java.net.URL;

/**
 * An abstract base class for jre modules that want to manually force runtime injection at bootstrap.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public class AbstractInjectionTest {

  private static final RunOnce once = new RunOnce();

  @BeforeClass
  public static void setProperties() {
    System.setProperty(X_Namespace.PROPERTY_RUNTIME_META, "build/classes/java/test");
    System.setProperty(X_Namespace.PROPERTY_TEST, "true");
  }

  @Before // can't do BeforeClass because we need to call this.getClass()
  public void prepareInjector(){
    System.setProperty(X_Namespace.PROPERTY_RUNTIME_META, "build/classes/java/test");
    System.setProperty(X_Namespace.PROPERTY_TEST, "true");
    // but, we only want to run injection once, so we use a simple lock
    if (once.shouldRun(false)) {
      URL location = getClass().getProtectionDomain().getCodeSource().getLocation();
      String testClasses = location.toExternalForm().split("xapi")[0];
      testClasses = testClasses.replace("file:", "");
      try {
        Object injector = getClass().getClassLoader()
          .loadClass("xapi.jre.inject.RuntimeInjector")
          .newInstance();

        final PlatformChecker checker = new PlatformChecker();
        injector.getClass().getMethod("writeMetaInfo", String.class, PlatformChecker.class, String.class, String.class)
          .invoke(injector,testClasses, checker, "META-INF/singletons", "META-INF/instances");
      } catch (Exception e) {
        System.err.println("[WARN]: Runtime injection failure for "+getClass()+"; " +
        		"You should include xapi-jre-inject on your test classpath.");
        e.printStackTrace();
      }
    }
  }
}
