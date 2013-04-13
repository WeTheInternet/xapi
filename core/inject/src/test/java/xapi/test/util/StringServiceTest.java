package xapi.test.util;

import org.junit.Assert;
import org.junit.Test;


import static xapi.util.X_String.*;

public class StringServiceTest {

  @Test
  public void testTiming() {
    System.out.println(toMetricSuffix(1));
    Assert.assertEquals("1.0", toMetricSuffix(1));
    Assert.assertEquals("100.0", toMetricSuffix(100));
    Assert.assertEquals("100.0 milli", toMetricSuffix(0.1));
    Assert.assertEquals("1.0 milli", toMetricSuffix(0.001));
    Assert.assertEquals("100.0 micro", toMetricSuffix(0.0001));
    Assert.assertEquals("1.0 micro", toMetricSuffix(0.000001));
    Assert.assertEquals("100.0 nano", toMetricSuffix(0.0000001));
    Assert.assertEquals("1.0 nano", toMetricSuffix(0.000000001));
    //test out of bounds
    Assert.assertEquals("0.1 nano", toMetricSuffix(0.0000000001));
    Assert.assertEquals("0.001 nano", toMetricSuffix(0.000000000001));

    Assert.assertEquals("1.0 kilo", toMetricSuffix(1000));
    Assert.assertEquals("100.0 kilo", toMetricSuffix(100000));
    Assert.assertEquals("1.0 mega", toMetricSuffix(1000000));
    Assert.assertEquals("100.0 mega", toMetricSuffix(100000000));
    Assert.assertEquals("1.0 giga", toMetricSuffix(1000000000));
    Assert.assertEquals("100.0 giga", toMetricSuffix(100000000000.0));
    //test out of bounds
    Assert.assertEquals("100000.0 giga", toMetricSuffix(100000000000000.0));
    Assert.assertEquals("1000000.0 giga", toMetricSuffix(1000000000000000.0));

  }
}
