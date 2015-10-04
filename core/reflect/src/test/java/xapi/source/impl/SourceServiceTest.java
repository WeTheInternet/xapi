package xapi.source.impl;

import org.junit.Assert;
import org.junit.Test;

import xapi.source.api.IsType;

public class SourceServiceTest {

  private static final String TEST_PACKAGE = "net.wetheinter.test";
  private static final String TEST_TYPE = "TestType";
  private static final String TEST_INNER_TYPE = "InnerType";
  final SourceServiceDefault service = getSourceService();

  protected SourceServiceDefault getSourceService() {
    return new SourceServiceDefault();
  }

  @Test
  public void testToType() {
    final IsType type = service.toType(TEST_PACKAGE, TEST_TYPE);
    Assert.assertEquals(type.getPackage(), TEST_PACKAGE);
    Assert.assertEquals(type.getSimpleName(), TEST_TYPE);
    Assert.assertEquals(type.getEnclosedName(), TEST_TYPE);
  }

  @Test
  public void testToTypeInner() {
    final IsType type = service.toType(TEST_PACKAGE, TEST_TYPE+"."+TEST_INNER_TYPE);
    Assert.assertEquals(type.getPackage(), TEST_PACKAGE);
    Assert.assertEquals(type.getSimpleName(), TEST_INNER_TYPE);
    Assert.assertEquals(type.getEnclosedName(), TEST_TYPE+"."+TEST_INNER_TYPE);
  }

  @Test
  public void testParentType() {
    final IsType parent = service.toType(TEST_PACKAGE, TEST_TYPE);
    final IsType type = service.toType(TEST_PACKAGE, TEST_TYPE+"."+TEST_INNER_TYPE);
    Assert.assertEquals(parent, type.getEnclosingType());
    Assert.assertTrue(parent == type.getEnclosingType());
  }

}
