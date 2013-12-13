package com.google.gwt.reflect.test;

import java.lang.reflect.Method;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.reflect.client.GwtReflect;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;

@ReflectionStrategy(debug=ReflectionStrategy.ALL, annotationRetention=ReflectionStrategy.ALL)
public class DemoTest extends GWTTestCase {

  private static final int finalField = 1;
  protected long instanceField = finalField;

  public DemoTest() {// so we can use getClass().newInstance
  }
  
  DemoTest(int value) {
    instanceField = value;
  }
  
  @Before
  public void sanityTest() {
    assertEquals(finalField, instanceField);
    instanceField = 0;
  }
  
  @After
  public void reset() {
    instanceField = (long)finalField;
  }

  @Test
  public void testMethodInvocation() throws Throwable {
    Method method = DemoTest.class.getDeclaredMethod("setInstanceField", int.class);
    method.setAccessible(true);
    method.invoke(this, 2.5);
    assertEquals(instanceField, 2);
  }
  
  @Test(expected=AssertionError.class)
  public void notRunInGwtTestCase() {
    fail();
  }
  
  @Test
  public void testClassAnnotation() {
    GwtReflect.magicClass(DemoTest.class);
    ReflectionStrategy strategy = DemoTest.class.getAnnotation(ReflectionStrategy.class);
    assertNotNull(strategy);
    assertEquals(strategy.debug(), ReflectionStrategy.ALL);
  }
  
  @Test
  public void testNewInstance() throws Throwable {
    DemoTest instance = DemoTest.class.newInstance();
    assertEquals(instance.instanceField, finalField);
    instance = DemoTest.class.getConstructor(int.class).newInstance(3);
    assertEquals(instance.instanceField, 3);
  }

  
  @SuppressWarnings("unused")
  private void setInstanceField(int value) {
    instanceField = new Long(value);
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.reflect.ReflectTest";
  }
  
}
