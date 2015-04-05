package com.google.gwt.reflect.legacy;

import static com.google.gwt.reflect.shared.GwtReflect.magicClass;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.reflect.test.AnnotationTests;
import com.google.gwt.reflect.test.ConstructorTests;
import com.google.gwt.reflect.test.FieldTests;
import com.google.gwt.reflect.test.JUnit4Test;
import com.google.gwt.reflect.test.MethodTests;

public class LegacyJUnitTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.reflect.ReflectTest";
  }

  public void testAnnotations() throws Throwable {
    magicClass(AnnotationTests.class);
    JUnit4Test.runTests(AnnotationTests.class);
  }

  public void testArrays() throws Throwable {
//    magicClass(ArrayTests.class);
//    JUnit4Test.runTests(ArrayTests.class);
  }

  public void testConstructors() throws Throwable {
    magicClass(ConstructorTests.class);
    JUnit4Test.runTests(ConstructorTests.class);
  }

  public void testFields() throws Throwable {
    magicClass(FieldTests.class);
    JUnit4Test.runTests(FieldTests.class);
  }

  public void testMethods() throws Throwable {
    magicClass(MethodTests.class);
    JUnit4Test.runTests(MethodTests.class);
  }

  public void testForDemo() throws Throwable {
    magicClass(DemoTest.class);
    JUnit4Test.runTests(DemoTest.class);
  }

}
