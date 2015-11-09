package com.google.gwt.reflect.test;

import static com.google.gwt.reflect.shared.GwtReflect.magicClass;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.reflect.client.ConstPool.ArrayConsts;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.shared.GwtReflect;
import com.google.gwt.reflect.test.cases.ReflectionCaseNoMagic;
import com.google.gwt.reflect.test.cases.ReflectionCaseSubclass;
import com.google.gwt.reflect.test.cases.ReflectionCaseSuperclass;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * @author James X. Nelson (james@wetheinter.net)
 */
@ReflectionStrategy(keepEverything=true)
public class MethodTests extends AbstractReflectionTest {

  private static final Class<Object> CLASS_OBJECT = Object.class;

  private static final Class<?> classInt = int.class;
  private static final Class<?> classLit = classList();
  private static final Class<ReflectionCaseSubclass> SUB_CLASS = magicClass(ReflectionCaseSubclass.class);
  private static final Class<ReflectionCaseSuperclass> SUPER_CLASS = magicClass(ReflectionCaseSuperclass.class);

  static {magicClass(MethodTests.class);}

  private static Class<?>[] classArray() {
    final Class<?>[] array = new Class<?>[]{classInt, classObject()};
    return array;
  }

  private static final Class<?> classList(){return List.class;}

  private static final Class<?> classObject(){return Object.class;}

  private static final native Class<?> nativeMethod()
  /*-{
   return @java.util.List::class;
   }-*/;


  public MethodTests() {}
  @Test(expected=NoSuchMethodException.class)
  public void testCantAccessPrivateMethods() throws Throwable  {
    final String preventMagicMethod = PRIVATE_MEMBER;
    try {
      SUPER_CLASS.getMethod(preventMagicMethod);
    } catch (final NoSuchMethodException e) {
      throw e;
    } catch (final Throwable e) {
      // GWT dev wraps it's reflection failures in InvocationTargetException :/
      if (GWT.isClient() && !GWT.isScript() && e instanceof InvocationTargetException) {
        throw e.getCause();
      }
      throw e;
    }
  }
  @Test
  public void testComplexMethodInjection() throws Exception {
    final List<String> list = new ArrayList<String>();
    Method method = ArrayList.class.getMethod("add", Object.class);
    method.invoke(list, "success");
    assertEquals("success", list.get(0));

    final Class<?>[] array = classArray();
    method = classList().getMethod("add", array);
    method.invoke(list, 0, "Success!");
    assertEquals("Success!", list.get(0));
  };
  @Test
  public void testDeclaredMethodDirectly() throws Throwable {
    final ReflectionCaseNoMagic superClass = new ReflectionCaseNoMagic();
    assertFalse(superClass.wasPrivateCalled());
    final Method m = NO_MAGIC.getDeclaredMethod(PRIVATE_MEMBER);
    m.setAccessible(true);
    assertNotNull(m);
    m.invoke(superClass);
    assertTrue(superClass.wasPrivateCalled());
  };
  @Test
  public void testDeclaredMethodInjectly() throws Throwable {
    final ReflectionCaseSuperclass superClass = new ReflectionCaseSuperclass();
    assertFalse(superClass.publicCall);
    final Method m = GwtReflect.getDeclaredMethod(SUPER_CLASS, PUBLIC_MEMBER);
    assertNotNull(m);
    m.invoke(superClass);
    assertTrue(superClass.publicCall);
  }
  @Test
  public void testGetPublicMethodDirectly() throws Throwable {
    final ReflectionCaseNoMagic noMagic = new ReflectionCaseNoMagic();
    final Method m = NO_MAGIC.getMethod(METHOD_EQUALS, Object.class);
    assertNotNull(m);
    assertTrue((Boolean)m.invoke(noMagic, noMagic));
  }
  @Test
  public void testGetPublicMethodInjectly() throws Throwable {
    final Method m = GwtReflect.getPublicMethod(NO_MAGIC, METHOD_EQUALS, CLASS_OBJECT);
    assertNotNull(m);
    assertFalse((Boolean)m.invoke(new ReflectionCaseNoMagic(), new ReflectionCaseNoMagic()));
  }

  @Test
  public void testInvoke() throws Throwable {
    final ReflectionCaseNoMagic inst = new ReflectionCaseNoMagic();
    assertFalse(inst.publicCall);
    assertFalse(inst.wasPrivateCalled());

    GwtReflect.invoke(NO_MAGIC, ReflectionCaseNoMagic.PUBLIC_CALL, ArrayConsts.EMPTY_CLASSES, inst, ArrayConsts.EMPTY_OBJECTS);
    assertTrue(inst.publicCall);
    assertFalse(inst.wasPrivateCalled());

    GwtReflect.invoke(NO_MAGIC, PRIVATE_MEMBER, ArrayConsts.EMPTY_CLASSES, inst, ArrayConsts.EMPTY_OBJECTS);
    assertTrue(inst.publicCall);
    assertTrue(inst.wasPrivateCalled());

  }

}
