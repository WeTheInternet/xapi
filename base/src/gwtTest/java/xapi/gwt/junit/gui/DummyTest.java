package xapi.gwt.junit.gui;

import org.junit.Test;
import xapi.test.Assert;

import com.google.gwt.core.client.GWT;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;

/**
 * Created by james on 16/10/15.
 */

@ReflectionStrategy(keepEverything=true)
public class DummyTest {
  @Test
  public void testStuff() {
    Assert.assertEquals(2, 1 + 1);
  }
  @Test
  public void failsOnGwtClient() {
    if (GWT.isClient()) {
      Assert.assertEquals(5, 2+2);
    }
  }
}
