package xapi.dev.gwtc;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import xapi.dev.X_Gwtc;
import xapi.dev.gwtc.api.GwtcService;
import xapi.gwtc.api.GwtManifest;
import xapi.test.Assert;
import xapi.test.gwtc.cases.CaseEntryPoint;
import xapi.test.gwtc.cases.GwtcCaseJunit4;
import xapi.util.X_Namespace;
import xapi.util.X_Properties;

import com.google.gwt.core.ext.TreeLogger.Type;

public class GwtcEntryPointTest {

  static {
    X_Properties.setProperty(X_Namespace.PROPERTY_MULTITHREADED, "10");
  }

  private static boolean beforeClass;
  private boolean before;

  public static void main(final String ... args) {
    new GwtcEntryPointTest().testEntryPointCompiles();
  }

  @BeforeClass
  public static void beforeClass() {
    beforeClass = true;
  }

  @AfterClass
  public static void afterClass() {
    beforeClass = false;
  }

  @Before
  public void before() {
    before = true;
  }

  @After
  public void after() {
    before = false;
  }

  @Test
  public void testBeforeAfter() {
    Assert.assertTrue(before);
    Assert.assertTrue(beforeClass);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testExpectedError() {
    throw new IllegalArgumentException();
  }
  @Test
  public void testPackageCompiles() {
    if ("true".equals(System.getProperty("xapi.build.quick"))) {
      return;
    }
    final Package pkg = CaseEntryPoint.class.getPackage();
    final GwtcService gwtc = X_Gwtc.getServiceFor(pkg, false);
    final GwtManifest manifest = new GwtManifest("Gwtc_"+pkg.getName().replace('.', '_'));
    manifest.setLogLevel(Type.TRACE);
    manifest.addSystemProp("gwt.usearchives=false");
    Assert.assertEquals(0, gwtc.compile(manifest));
  }
  @Test
  public void testEntryPointCompiles() {
    if ("true".equals(System.getProperty("xapi.build.quick"))) {
      return;
    }
    final GwtcService gwtc = X_Gwtc.getServiceFor(CaseEntryPoint.class);
    gwtc.addJUnitClass(GwtcCaseJunit4.class);
    final GwtManifest manifest = new GwtManifest(gwtc.getModuleName());
    manifest.setLogLevel(Type.TRACE);
    manifest.addSystemProp("gwt.usearchives=false");
    Assert.assertEquals(0, gwtc.compile(manifest));
  }

}
