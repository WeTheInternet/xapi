package xapi.gwt.test;

import junit.framework.Assert;
import xapi.gwt.rebind.MagicMethodTestGenerator;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * A simple test to ensure magic method generation is functioning correctly.
 * <p>
 * See {@link MagicMethodTestGenerator} for implementation details.
 * <p>
 * @author James X. Nelson (james@wetheinter.net, @james)
 */
public class MagicMethodGwtTest extends GWTTestCase{


  private static final int devMode = 1;
  private static final int prodMode = 0;
  private static boolean calledMagically = false;

  // Called from generator.  Should happen before entry point is loaded.
  public static void callFromGenerator() {
    calledMagically = true;
  }

  public MagicMethodGwtTest() {
    // We run this test in the constructor,
    // to prove our method call was inserted before our entry point.
    Assert.assertEquals(GWT.isProdMode(), calledMagically);
  }

  public static int replaceMe() {
    return devMode;
  }

  @Override
  public String getModuleName() {
    return "xapi.gwt.MagicMethodTest";
  }

  public void testMethodReplacement() {
    Assert.assertEquals(GWT.isProdMode() ? prodMode : devMode, replaceMe());
  }

}
