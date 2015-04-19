/**
 *
 */
package com.google.gwt.reflect.jvm;

import com.google.gwt.reflect.rebind.ReflectionUtilJava;
import com.google.gwt.thirdparty.xapi.dev.source.MemberBuffer;
import com.google.gwt.thirdparty.xapi.dev.source.SourceBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
@SuppressWarnings("unused")
public class TypeImportTest {

  static class TestClass <T extends TypeImportTest> {
  }

  private MemberBuffer<?> buffer;
  private SourceBuilder<Object> sources;

  @Before
  public void initBuffer() {
    sources = new SourceBuilder<Object>("class Test");
    buffer = sources.getClassBuffer();
  }

  Class<? extends TestClass> cls = TestClass.class;

  private <T extends Annotation> T testSimpleGenericMethod() {
    return null;
  }

  private <T extends Annotation> Class<? extends T> testComplexGenericMethod() {
    return null;
  }

  @Test
  public void testImportFieldType() {
    final String val = ReflectionUtilJava.toSourceName(cls.getClass(), buffer);
    Assert.assertEquals("Class<T extends Object> ", val);
    Assert.assertEquals(
        "\nclass Test {"
        + "\n"
        + "\n}"
        + "\n", sources.toString());
  }

  @Test
  public void testImportMethodType() throws Throwable {
    final Method method = TypeImportTest.class.getDeclaredMethod("testSimpleGenericMethod");
    final String val = ReflectionUtilJava.toSourceName(method.getGenericReturnType(), buffer);
    Assert.assertEquals("T extends Annotation", val);
    Assert.assertEquals(
        "import java.lang.annotation.Annotation;"
        + "\n"
        + "\nclass Test {"
        + "\n"
        + "\n}"
        + "\n", sources.toString());
  }

  @Test
  public void testImportWildcardType() throws Throwable {
    final Method method = TypeImportTest.class.getDeclaredMethod("testComplexGenericMethod");
    final String val = ReflectionUtilJava.toSourceName(method.getGenericReturnType(), buffer);
    Assert.assertEquals("Class<? extends Annotation> ", val);
    Assert.assertEquals(
      "import java.lang.annotation.Annotation;"
      + "\n"
      + "\nclass Test {"
      + "\n"
      + "\n}"
      + "\n", sources.toString());
  }

}
