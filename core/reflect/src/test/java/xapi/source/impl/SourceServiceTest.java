package xapi.source.impl;

import org.junit.Assert;
import org.junit.Test;

import xapi.source.api.IsType;

public class SourceServiceTest {

  private static final String TEST_PACKAGE = "net.wetheinter.test";
  private static final String TEST_TYPE = "TestType";
  private static final String TEST_INNER_TYPE = "InnerType";
  final SourceServiceDefault service = new SourceServiceDefault();

  @Test
  public void testToType() {
    IsType type = service.toType(TEST_PACKAGE, TEST_TYPE);
    Assert.assertEquals(type.getPackage(), TEST_PACKAGE);
    Assert.assertEquals(type.getSimpleName(), TEST_TYPE);
    Assert.assertEquals(type.getEnclosedName(), TEST_TYPE);
  }

  @Test
  public void testToTypeInner() {
    IsType type = service.toType(TEST_PACKAGE, TEST_TYPE+"."+TEST_INNER_TYPE);
    Assert.assertEquals(type.getPackage(), TEST_PACKAGE);
    Assert.assertEquals(type.getSimpleName(), TEST_INNER_TYPE);
    Assert.assertEquals(type.getEnclosedName(), TEST_TYPE+"."+TEST_INNER_TYPE);
  }

}
