package com.google.gwt.reflect.test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.reflect.client.GwtReflect;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.test.annotations.CompileRetention;
import com.google.gwt.reflect.test.annotations.RuntimeRetention;
import com.google.gwt.reflect.test.cases.ReflectionCaseHasAllAnnos;
import com.google.gwt.reflect.test.cases.ReflectionCaseSimple;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 */
@ReflectionStrategy(magicSupertypes=false, keepCodeSource=true)
public class FieldTests extends AbstractReflectionTest {
  
  public FieldTests() {}

  @Test
  public void testSimpleReflection() throws Exception {
    Class<ReflectionCaseSimple> c = GwtReflect.magicClass(ReflectionCaseSimple.class);
    ReflectionCaseSimple inst = testNewInstance(c);
    ReflectionCaseSimple anon = new ReflectionCaseSimple() {};
    testAssignable(inst, anon);

    testHasNoArgDeclaredMethods(c, "privatePrimitive", "privateObject", "publicPrimitive", "publicObject");
    testHasNoArgPublicMethods(c, "publicPrimitive", "publicObject", "hashCode", "toString");
    testCantAccessNonPublicMethods(c, "privatePrimitive", "privateObject");
    testCantAccessNonDeclaredMethods(c, "hashCode", "toString");
  }

  @Test
  public void testAnnotationsKeepAll() throws Exception {
    Class<?> testCase = GwtReflect.magicClass(ReflectionCaseHasAllAnnos.class);
    Field field = testCase.getDeclaredField("field");
    Method method = testCase.getDeclaredMethod("method", Long.class);
    Constructor<?> ctor = testCase.getDeclaredConstructor(long.class);

    Annotation[] annos = testCase.getAnnotations();
    assertHasAnno(testCase, annos, RuntimeRetention.class);
    if (GWT.isScript()) {
      // Gwt Dev can only access runtime level retention annotations
      assertHasAnno(testCase, annos, CompileRetention.class);
    }
    annos = field.getAnnotations();
    assertHasAnno(testCase, annos, RuntimeRetention.class);
    if (GWT.isScript()) {
      // Gwt Dev can only access runtime level retention annotations
      assertHasAnno(testCase, annos, CompileRetention.class);
    }

    annos = method.getAnnotations();
    assertHasAnno(testCase, annos, RuntimeRetention.class);
    if (GWT.isScript()) {
      // Gwt Dev can only access runtime level retention annotations
      assertHasAnno(testCase, annos, CompileRetention.class);
    }

    annos = ctor.getAnnotations();
    assertHasAnno(testCase, annos, RuntimeRetention.class);
    if (GWT.isScript()) {
      // Gwt Dev can only access runtime level retention annotations
      assertHasAnno(testCase, annos, CompileRetention.class);
    }

  }

  private static final Class<?> classInt = int.class;
  private static final Class<?> classList = classList();
  private static final Class<?> classObject(){return Object.class;};
  private static final Class<?> classList(){return List.class;};
  private static final native Class<?> nativeMethod()
  /*-{
   return @java.util.List::class;
   }-*/;
  private static Class<?>[] classArray() {
    final Class<?>[] array = new Class<?>[]{classInt, classObject()};
    return array;
  }

  @Test(timeout=3000)
  public void testDirectMethodInjection() throws Exception {
    ArrayList<String> list = new ArrayList<String>();
    Method method = ArrayList.class.getMethod("add", Object.class);
    method.invoke(list, "success");
    assertEquals("success", list.get(0));

    final Class<?>[] array = classArray();
    method = classList.getMethod("add", array);
    method.invoke(list, 0, "Success!");
    assertEquals("Success!", list.get(0));
  }

  private void assertHasAnno(Class<?> cls, Annotation[] annos, Class<? extends Annotation> annoClass) {
    for (Annotation anno : annos ) {
      if (anno.annotationType() == annoClass)
        return;
    }
    fail(cls.getName()+" did not have required annotation "+annoClass);
  }

  private void testCantAccessNonPublicMethods(Class<?> c, String ... methods) {
    for (String method : methods) {
      try {
        c.getMethod(method);
        fail("Could erroneously access non-public method "+method+" in "+c.getName());
      } catch (NoSuchMethodException e) {}
    }
  }

  private void testCantAccessNonDeclaredMethods(Class<?> c, String ... methods) {
    for (String method : methods) {
      try {
        c.getDeclaredMethod(method);
        fail("Could erroneously access non-declared method "+method+" in "+c.getName());
      } catch (NoSuchMethodException e) {}
    }
  }

  private void testHasNoArgDeclaredMethods(Class<?> c, String ... methods) throws Exception{
    for (String method : methods) {
      assertNotNull(c.getDeclaredMethod(method));
    }
  }
    private void testHasNoArgPublicMethods(Class<?> c, String ... methods) throws Exception{
      for (String method : methods) {
        assertNotNull(c.getMethod(method));
      }
  }

  private void testAssignable(Object inst, Object anon) {
    assertTrue(inst.getClass().isAssignableFrom(anon.getClass()));
    assertFalse(anon.getClass().isAssignableFrom(inst.getClass()));
  }

  private <T> T testNewInstance(Class<T> c) throws Exception {
    T newInst = c.newInstance();
    assertNotNull(c.getName()+" returned null instead of a new instance", newInst);
    assertTrue(c.isAssignableFrom(newInst.getClass()));
    return newInst;
  }

}
