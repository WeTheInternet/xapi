package xapi.dev.gwtc.test;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import xapi.dev.X_Gwtc;
import xapi.dev.gwtc.api.GwtcService;
import xapi.dev.gwtc.test.cases.CaseEntryPoint;
import xapi.gwtc.api.GwtManifest;
import xapi.test.Assert;
import xapi.util.X_Namespace;
import xapi.util.X_Properties;

public class GwtcEntryPointTest {

  static {
    X_Properties.setProperty(X_Namespace.PROPERTY_MULTITHREADED, "10");
  }
  
  private static boolean beforeClass;
  private boolean before;

  public static void main(String ... args) {
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
  
  @Test(expected=IllegalArgumentException.class)
  public void testExpectedError() {
    throw new IllegalArgumentException();
  }
  @Test
  public void testEntryPointCompiles() {
    GwtcService gwtc = X_Gwtc.getServiceFor(CaseEntryPoint.class);
    GwtManifest manifest = new GwtManifest(gwtc.getModuleName());
    Assert.assertEquals(0, gwtc.compile(manifest));
  }
  
}
