package com.google.gwt.reflect.test;

import static com.google.gwt.reflect.test.AbstractReflectionTest.assertEquals;
import static com.google.gwt.reflect.test.AbstractReflectionTest.assertFalse;
import static com.google.gwt.reflect.test.AbstractReflectionTest.assertTrue;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.reflect.shared.JsMemberPool;

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

public class JUnit4Test {

  private class Lifecycle {
    Map<String, Method> after = newMap();
    Map<String, Method> afterClass = newMap();
    Map<String, Method> before = newMap();
    Map<String, Method> beforeClass = newMap();
    Map<String, Method> tests = newMap();

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    public Lifecycle(final Class testClass) {
      Class init = testClass;
      while (init != null && init != Object.class) {
        try {
          JsMemberPool.getMembers(init);
          for (final Method method : init.getDeclaredMethods()) {
            if (method.getAnnotation(Test.class) != null) {
              assertPublicZeroArgInstanceMethod(method, Test.class);
              if (!tests.containsKey(method.getName())) {
                tests.put(method.getName(), method);
              }
            }
            maybeAdd(method);
          }
        } catch (final NoSuchMethodError ignored) {
          debug("Class "+init+" is not enhanced", null);
        } catch (final Exception e) {
          debug("Error getting declared methods for "+init, e);
        }
        init = init.getSuperclass();
      }
    }

    public List<Method> after() {
      return newList(after, true);
    }

    public List<Method> afterClass() {
      return newList(afterClass, true);
    }

    public List<Method> before() {
      return newList(before, true);
    }

    public List<Method> beforeClass() {
      return newList(beforeClass, true);
    }

    public void maybeAdd(final Method method) {
      if (method.getAnnotation(Before.class) != null) {
        assertPublicZeroArgInstanceMethod(method, Before.class);
        if (!before.containsKey(method.getName())) {
          before.put(method.getName(), method);
        }
      }
      if (method.getAnnotation(BeforeClass.class) != null) {
        assertPublicZeroArgStaticMethod(method, BeforeClass.class);
        if (!beforeClass.containsKey(method.getName())) {
          beforeClass.put(method.getName(), method);
        }
      }
      if (method.getAnnotation(After.class) != null) {
        assertPublicZeroArgInstanceMethod(method, After.class);
        if (!after.containsKey(method.getName())) {
          after.put(method.getName(), method);
        }
      }
      if (method.getAnnotation(AfterClass.class) != null) {
        assertPublicZeroArgStaticMethod(method, AfterClass.class);
        if (!afterClass.containsKey(method.getName())) {
          afterClass.put(method.getName(), method);
        }
      }
    }

  }

  public static Method[] findTests(final Class<?> testClass) throws Throwable {
    return new JUnit4Test().findAnnotated(testClass);
  }

  public static void runTest(final Object inst, final Method m) throws Throwable {
    new JUnit4Test().run(inst, m);
  }

  public static void runTests(final Class<?> testClass) throws Throwable {
    new JUnit4Test().runAll(testClass);
  }

  private Method[] findAnnotated(final Class<?> testClass) {
    final Lifecycle lifecycle = new Lifecycle(testClass);
    return lifecycle.tests.values().toArray(new Method[0]);
  }

  protected void assertPublicZeroArgInstanceMethod(final Method method, final Class<?> type) {
    assertPublicZeroArgMethod(method, type);
    assertFalse("@" + type.getSimpleName() + " methods must not be static",
        Modifier.isStatic(method.getModifiers()));
  }

  protected void assertPublicZeroArgMethod(final Method method, final Class<?> type) {
    assertTrue("@" + type.getSimpleName() + " methods must be public",
        Modifier.isPublic(method.getModifiers()));
    assertEquals("@" + type.getSimpleName() + " methods must be zero-arg",
        0, method.getParameterTypes().length);
  }

  protected void assertPublicZeroArgStaticMethod(final Method method, final Class<?> type) {
    assertPublicZeroArgMethod(method, type);
    assertTrue("@" + type.getSimpleName() + " methods must be static",
        Modifier.isStatic(method.getModifiers()));
  }
  protected void debug(final String string, Throwable e) {
    if (GWT.isProdMode()) {
      GWT.log(string+" ("+e+")");
    } else {
      System.out.println(string);
    }
    while (e != null) {
      e.printStackTrace(System.err);
      e = e.getCause();
    }
  }

  protected void execute(final Object inst, final Map<String, Method> tests, final List<Method> beforeClass,
      final List<Method> before, final List<Method> after, final List<Method> afterClass)
      throws TestsFailed, IllegalArgumentException, IllegalAccessException, InvocationTargetException, NoSuchMethodError {
    final Map<Method, Throwable> result = new LinkedHashMap<Method, Throwable>();
    try {

      for (final Method m : beforeClass) {
        m.invoke(null);
      }

      for (final Entry<String, Method> test : tests.entrySet()) {
        result.put(test.getValue(), runTest(inst, test.getValue(), before, after));
      }

    } finally {

      for (final Method m : afterClass) {
        m.invoke(null);
      }

    }
    for (final Entry<Method, Throwable> e : result.entrySet()) {
      if (e.getValue() != null) {
        final TestsFailed failure = new TestsFailed(result);
        debug("Tests Failed;\n", failure);
        throw new AssertionError(failure.toString());
      }
    }
  }

  protected List<Method> newList(final Map<String, Method> beforeClass, final boolean reverse) {
    List<Method> list;
    if (reverse) {
      list = new LinkedList<Method>();
      for (final Entry<String, Method> e : beforeClass.entrySet()) {
        list.add(0, e.getValue());
      }
    } else {
      list = new ArrayList<Method>();
      for (final Entry<String, Method> e : beforeClass.entrySet()) {
        list.add(e.getValue());
      }
    }
    return list;
  }

  protected Map<String, Method> newMap() {
    return new LinkedHashMap<String, Method>();
  }
  protected void run(final Object inst, final Method m) throws Throwable {
    final Lifecycle lifecycle = new Lifecycle(m.getDeclaringClass());
    lifecycle.tests.clear();
    lifecycle.tests.put(m.getName(), m);
    execute(inst, lifecycle.tests,
        lifecycle.beforeClass(), lifecycle.before(), lifecycle.after(), lifecycle.afterClass());
  }

  protected void runAll(final Class<?> testClass) throws Throwable {
    final Lifecycle lifecycle = new Lifecycle(testClass);
    if (lifecycle.tests.size() > 0) {
      final Object inst = testClass.newInstance();
      execute(inst, lifecycle.tests, lifecycle.beforeClass(), lifecycle.before(), lifecycle.after(), lifecycle.afterClass());
    }
  }

  protected Throwable runTest(final Object inst, final Method value, final List<Method> before, final List<Method> after) {

    Test test = null;
    try {
      test = value.getAnnotation(Test.class);
    } catch (final Exception e) {
      debug("Error getting @Test annotation",e);
    }
    final Class<? extends Throwable> expected = test == null ? Test.None.class : test.expected();
    // We'll have to figure out timeouts in the actual JUnit jvm

    try {
      debug("Executing "+value+" on "+inst, null);
      for (final Method m : before) {
        m.invoke(inst);
      }
      try {
        value.invoke(inst);
      } catch (final InvocationTargetException e) {
        throw e.getCause();
      }

      if (expected != Test.None.class) {
        return new AssertionError("Method "+value+" was supposed to throw "+expected.getName()
            +", but failed to do so");
      }
      return null;
    } catch (final Throwable e) {
      return expected.isAssignableFrom(e.getClass()) ? null : e;
    } finally {
      for (final Method m : after) {
        try {
          m.invoke(inst);
        } catch (final Throwable e) {
          debug("Error calling after methods", e);
          return e;
        }
      }
    }
  }

}
