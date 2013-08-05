package com.google.gwt.reflect.test;

import static com.google.gwt.reflect.test.AbstractReflectionTest.assertEquals;
import static com.google.gwt.reflect.test.AbstractReflectionTest.assertFalse;
import static com.google.gwt.reflect.test.AbstractReflectionTest.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.client.Window;

public class JUnit4Test {
  
  private class Lifecycle {
    Map<String, Method> after = newMap();
    Map<String, Method> before = newMap();
    Map<String, Method> afterClass = newMap();
    Map<String, Method> beforeClass = newMap();
    Map<String, Method> tests = newMap();
    
    @SuppressWarnings("rawtypes")
    public Lifecycle(Class testClass) {
      Class init = testClass;
      while (init != null) {
        try {
          for (Method method : init.getDeclaredMethods()) {
            if (method.getAnnotation(Test.class) != null) {
              assertPublicZeroArgInstanceMethod(method, Test.class);
              if (!tests.containsKey(method.getName()))
                tests.put(method.getName(), method);
            }
            maybeAdd(method);
          }
        } catch (NoSuchMethodError ignored) {
          debug("Class "+init+" is not enhanced", null);
        } catch (Exception e) {
          debug("Error getting declared methods for "+init, e);
        }
        init = init.getSuperclass();
      }
    }

    public void maybeAdd(Method method) {
      if (method.getAnnotation(Before.class) != null) {
        assertPublicZeroArgInstanceMethod(method, Before.class);
        if (!before.containsKey(method.getName()))
          before.put(method.getName(), method);
      }
      if (method.getAnnotation(BeforeClass.class) != null) {
        assertPublicZeroArgStaticMethod(method, BeforeClass.class);
        if (!beforeClass.containsKey(method.getName()))
          beforeClass.put(method.getName(), method);
      }
      if (method.getAnnotation(After.class) != null) {
        assertPublicZeroArgInstanceMethod(method, After.class);
        if (!after.containsKey(method.getName()))
          after.put(method.getName(), method);
      }
      if (method.getAnnotation(AfterClass.class) != null) {
        assertPublicZeroArgStaticMethod(method, AfterClass.class);
        if (!afterClass.containsKey(method.getName()))
          afterClass.put(method.getName(), method);
      }
    }

    public List<Method> beforeClass() {
      return newList(beforeClass, true);
    }

    public List<Method> before() {
      return newList(before, true);
    }

    public List<Method> after() {
      return newList(after, true);
    }

    public List<Method> afterClass() {
      return newList(afterClass, true);
    }

  }

  public static Method[] findTests(Class<?> testClass) throws Throwable {
    return new JUnit4Test().findAnnotated(testClass);
  }

  private Method[] findAnnotated(Class<?> testClass) {
    Lifecycle lifecycle = new Lifecycle(testClass);
    return lifecycle.tests.values().toArray(new Method[0]);
  }

  public static void runTests(Class<?> testClass) throws Throwable {
    new JUnit4Test().runAll(testClass);
  }

  public static void runTest(Object inst, Method m) throws Throwable {
    new JUnit4Test().run(inst, m);
  }

  protected void run(Object inst, Method m) throws Throwable {
    Lifecycle lifecycle = new Lifecycle(m.getDeclaringClass());
    lifecycle.tests.clear();
    lifecycle.tests.put(m.getName(), m);
    execute(inst, lifecycle.tests, 
        lifecycle.beforeClass(), lifecycle.before(), lifecycle.after(), lifecycle.afterClass());
  }

  protected void runAll(Class<?> testClass) throws Throwable {
    Lifecycle lifecycle = new Lifecycle(testClass);
    if (lifecycle.tests.size() > 0) {
      Object inst = testClass.newInstance();
      execute(inst, lifecycle.tests, lifecycle.beforeClass(), lifecycle.before(), lifecycle.after(), lifecycle.afterClass());
    }
  }

  protected Map<String, Method> newMap() {
    return new LinkedHashMap<String, Method>();
  }
  protected List<Method> newList(Map<String, Method> beforeClass, boolean reverse) {
    List<Method> list;
    if (reverse) {
      list = new LinkedList<Method>();
      for (Entry<String, Method> e : beforeClass.entrySet()) {
        list.add(0, e.getValue());
      }
    } else {
      list = new ArrayList<Method>();
      for (Entry<String, Method> e : beforeClass.entrySet()) {
        list.add(e.getValue());
      }
    }
    return list;
  }

  protected void execute(Object inst, Map<String, Method> tests, List<Method> beforeClass,
      List<Method> before, List<Method> after, List<Method> afterClass) 
      throws TestsFailed, IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchMethodError {
    Map<Method, Throwable> result = new LinkedHashMap<Method, Throwable>();
    try {
    
      for (Method m : beforeClass)
        m.invoke(null);
      
      for (Entry<String, Method> test : tests.entrySet()) {
        result.put(test.getValue(), runTest(inst, test.getValue(), before, after));
      }
      
    } finally {
      
      for (Method m : afterClass)
        m.invoke(null);
      
    }
    for (Entry<Method, Throwable> e : result.entrySet()) {
      if (e.getValue() != null) {
        TestsFailed failure = new TestsFailed(result);
        debug("Tests Failed; ", failure);
        throw new AssertionError(failure.toString());
      }
    }
  }

  protected Throwable runTest(Object inst, Method value, List<Method> before, List<Method> after) {
    
    Test test = value.getAnnotation(Test.class);
    Class<? extends Throwable> expected = test == null ? Test.None.class : test.expected();
    // We'll have to figure out timeouts in the actual JUnit jvm
    
    try {
      debug("Executing "+value+" on "+inst, null);
      for (Method m : before) {
        m.invoke(inst);
      }
      value.invoke(inst);
      if (expected != Test.None.class)
        return new AssertionError("Method "+value+" was supposed to throw "+expected.getName()
            +", but failed to do so");
      return null;
    } catch (Throwable e) {
      return expected.isAssignableFrom(e.getClass()) ? null : e;
    } finally {
      for (Method m : after) {
        try {
          m.invoke(inst);
        } catch (Throwable e) {
          debug("Error calling after methods", e);
          return e;
        }
      }
    }
  }

  protected void assertPublicZeroArgMethod(Method method, Class<?> type) {
    assertTrue("@" + type.getSimpleName() + " methods must be public",
        Modifier.isPublic(method.getModifiers()));
    assertEquals("@" + type.getSimpleName() + " methods must be zero-arg",
        0, method.getParameterTypes().length);
  }
  protected void assertPublicZeroArgInstanceMethod(Method method, Class<?> type) {
    assertPublicZeroArgMethod(method, type);
    assertFalse("@" + type.getSimpleName() + " methods must not be static",
        Modifier.isStatic(method.getModifiers()));
  }

  protected void assertPublicZeroArgStaticMethod(Method method, Class<?> type) {
    assertPublicZeroArgMethod(method, type);
    assertTrue("@" + type.getSimpleName() + " methods must be static",
        Modifier.isStatic(method.getModifiers()));
  }

  protected void debug(String string, Throwable e) {
    if (GWT.isProdMode()) {
      GWT.log(string+" ("+e+")");
    }
    else
      System.out.println(string);
    while (e != null) {
      e.printStackTrace(System.err);
      e = e.getCause();
    }
  }
  
}
