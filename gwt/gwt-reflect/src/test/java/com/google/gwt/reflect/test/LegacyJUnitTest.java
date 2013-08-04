package com.google.gwt.reflect.test;

import static com.google.gwt.reflect.client.GwtReflect.magicClass;

import com.google.gwt.junit.client.GWTTestCase;

public class LegacyJUnitTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.reflect.ReflectTest";
  }

  public void testSimpleReflection() throws Exception {
    new FieldTests().testSimpleReflection();
  }

  public void testAnnotationsKeepAll() throws Exception {
    new FieldTests().testAnnotationsKeepAll();
  }

  public void testDirectMethodInjection() throws Exception {
    new FieldTests().testDirectMethodInjection();
  }

  public void testAnnotations() throws Throwable {
    magicClass(AnnotationTests.class);
    JUnit4Test.runTests(AnnotationTests.class);
  }

  public void testMethods() throws Throwable {
    magicClass(MethodTests.class);
    JUnit4Test.runTests(MethodTests.class);
  }

}
