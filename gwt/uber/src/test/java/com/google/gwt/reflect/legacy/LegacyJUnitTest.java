package com.google.gwt.reflect.legacy;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;

import com.google.gwt.junit.client.GWTTestCase;

import java.util.concurrent.Callable;
//import com.google.gwt.reflect.test.AnnotationTests;
//import com.google.gwt.reflect.test.ArrayTests;
//import com.google.gwt.reflect.test.ConstructorTests;
//import com.google.gwt.reflect.test.FieldTests;
//import com.google.gwt.reflect.test.MethodTests;

public class LegacyJUnitTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.reflect.ReflectTest";
  }
//
//  @Override
//  public String getModuleName() {
//    return "xapi.X_Core";
//  }

  public void testI() {
    final IntTo<Callable> list = X_Collect.newList(Callable.class);
    list.add(()->null);
  }

//
//  public void testAnnotations() throws Throwable {
//    magicClass(AnnotationTests.class);
//    JUnit4Executor.runTests(AnnotationTests.class);
//  }
//
//  public void testArrays() throws Throwable {
//    magicClass(ArrayTests.class);
//    JUnit4Executor.runTests(ArrayTests.class);
//  }
//
//  public void testConstructors() throws Throwable {
//    magicClass(ConstructorTests.class);
//    JUnit4Executor.runTests(ConstructorTests.class);
//  }
//
//  public void testFields() throws Throwable {
//    magicClass(FieldTests.class);
//    JUnit4Executor.runTests(FieldTests.class);
//  }
//
//  public void testMethods() throws Throwable {
//    magicClass(MethodTests.class);
//    JUnit4Executor.runTests(MethodTests.class);
//  }
//
//  public void testForDemo() throws Throwable {
//    magicClass(DemoTest.class);
//    JUnit4Executor.runTests(DemoTest.class);
//  }

}
