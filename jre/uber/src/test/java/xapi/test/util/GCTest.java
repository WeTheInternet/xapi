package xapi.test.util;

import org.junit.Assert;
import org.junit.Test;

import xapi.annotation.gc.OnGC;
import xapi.test.AbstractInjectionTest;
import xapi.time.X_Time;
import xapi.debug.X_GC;

@OnGC(
    chainDeleteFields = true
    ,deleteInstanceFields = true
    ,staticGCMethods = "xapi.test.util.GCTest$Utils#destroy"
    ,instanceGCMethods = "destroy"
    )
public class GCTest extends AbstractInjectionTest{

  //Won't get destroyed, but lets us check that it's fields were cleared
  private static final Utils TheUtils = new Utils();
  
  // Some fields to destroy
  Utils util = TheUtils; // will be null
  int primitive = 10; // won't be touched (primitives)
  boolean destroyed = false; // ditto
  static boolean destroyed_static = false; // gets called in static destroy
  String str = "hasValue"; // will be null
  
  // Tests GC chaining
  @OnGC(deleteInstanceFields=true)
  static class Utils {
    // will be destroyed
    Object object = new Object();

    // will get called first
    public static void destroy(final Object o) {
      destroyed_static = true;
      // make sure we get called first
      Assert.assertNotNull(((GCTest)o).util);
      X_Time.runLater(new Runnable() {
        @Override
        public void run() {
          // wait a bit, since run later can't promise how much later;
          // 10 millis is plenty to wait from static destroy 'til fields cleared.
          X_Time.trySleep(10, 0);
          Assert.assertNull(TheUtils.object);
          Assert.assertNull(((GCTest)o).util);
        }
      });
    }
  }
  
  @Test
  public void testGC() {
    X_GC.destroy(GCTest.class, this);
    Assert.assertNull(str);
    Assert.assertTrue(destroyed);
    Assert.assertTrue(destroyed_static);
    // primitives don't get gc'd
    Assert.assertEquals(primitive, 10);
    // give time for Utils to run its assert
    X_Time.trySleep(500, 0);
  }
  
  public void destroy() {
    destroyed = true;
  }
  
}
