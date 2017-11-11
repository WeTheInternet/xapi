package com.google.gwt.reflect.legacy;

import static com.google.gwt.reflect.shared.GwtReflect.magicClass;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.gwt.junit.impl.JUnit4Executor;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.reflect.test.AnnotationTests;
import com.google.gwt.reflect.test.ArrayTests;
import com.google.gwt.reflect.test.ConstructorTests;
import com.google.gwt.reflect.test.FieldTests;
import com.google.gwt.reflect.test.MethodTests;

import java.util.concurrent.Callable;


public class LegacyJUnitTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.reflect.ReflectTest";
  }

  public void testIntTo() {
    // This is a xapi list type which produces new arrays of the correct type,
    // so you don't have to write collection.toArray(T[]::new); // fails on generics.
    // We have an injector which ensures the component type is registered w/ gwt array reflection,
    // but otherwise functions as a standard (emulated) call to Array.newInstance.
    final IntTo<Callable> list = X_Collect.newList(Callable.class);
    list.add(()->null);
  }

  public void testAnnotations() throws Throwable {
    magicClass(AnnotationTests.class);
    JUnit4Executor.runTests(AnnotationTests.class);
  }

  public void testArrays() throws Throwable {
    magicClass(ArrayTests.class);
    JUnit4Executor.runTests(ArrayTests.class);
  }

  public void testConstructors() throws Throwable {
    magicClass(ConstructorTests.class);
    JUnit4Executor.runTests(ConstructorTests.class);
  }

  public void testFields() throws Throwable {
    magicClass(FieldTests.class);
    JUnit4Executor.runTests(FieldTests.class);
  }

  public void testMethods() throws Throwable {
    magicClass(MethodTests.class);
    JUnit4Executor.runTests(MethodTests.class);
  }

  public void testForDemo() throws Throwable {
    magicClass(DemoTest.class);
    JUnit4Executor.runTests(DemoTest.class);
  }

}
