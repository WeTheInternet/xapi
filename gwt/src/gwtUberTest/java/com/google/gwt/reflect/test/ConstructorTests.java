package com.google.gwt.reflect.test;

import com.google.gwt.reflect.shared.GwtReflect;
import com.google.gwt.reflect.test.cases.ReflectionCaseNoMagic;

import java.lang.reflect.Constructor;

import org.junit.Test;

public class ConstructorTests extends AbstractReflectionTest {

   public static final Class<? extends SuperCase>
    CLS_SUPER_CASE = GwtReflect.magicClass(SuperCase.class),
    // We purposely want to erase the SubCase to SuperCase,
    // to make sure the field type does not influence our lookup.
    CLS_SUB_CASE = GwtReflect.magicClass(SubCase.class);


  @SuppressWarnings("rawtypes")
  private static final Class[]
    CLS_LONG = new Class[]{long.class},
    CLS_STRING = new Class[]{String.class};

  protected static class SuperCase {
    Object value;
     SuperCase() {
      value = this;
    }

    SuperCase(final String s) {
      value = s;
    }

    private SuperCase(final long j) {
      value = j;
    }

    // Here to make sure sub classes can't find super class public constructors
    public SuperCase(final Enum<?> e) {
      value = e;
    }

  }

  @SuppressWarnings("unused")
  protected static class SubCase extends SuperCase {
    public SubCase(final long l) {
      super(l+1);// to differentiate between super and sub class
    }
    private SubCase(final String s) {
      super(s+"1");
    }
  }

  @Test
  public void testSimpleCtor() throws Throwable {
    SuperCase inst;
    Constructor<? extends SuperCase> ctor;

    ctor = CLS_SUPER_CASE.getDeclaredConstructor(CLS_LONG);
    ctor.setAccessible(true);
    inst = ctor.newInstance(1);
    assertNotNull(inst);
    assertEquals(1L, inst.value);
    assertNotEquals(1, inst.value);

    ctor = CLS_SUPER_CASE.getDeclaredConstructor(CLS_STRING);
    ctor.setAccessible(true);
    inst = ctor.newInstance("1");
    assertNotNull(inst);
    assertEquals("1", inst.value);
  }

  @Test
  public void testSubCaseConstructor() throws Throwable {
    SuperCase inst;
    Constructor<? extends SuperCase> ctor;

    ctor = CLS_SUB_CASE.getConstructor(CLS_LONG);
    inst = ctor.newInstance(1);
    assertNotNull(inst);
    assertEquals(2L, inst.value);// sub class adds 1 to values

    ctor = CLS_SUB_CASE.getDeclaredConstructor(CLS_STRING);
    ctor.setAccessible(true);
    inst = ctor.newInstance("1");
    assertNotNull(inst);
    assertEquals("11", inst.value);

  }

  @Test
  public void testMagicSuperCase() throws Throwable {
    SuperCase inst;
    inst = GwtReflect.construct(CLS_SUPER_CASE, CLS_LONG, 1L);
    assertNotNull(inst);
    assertEquals(1L, inst.value);

    inst = GwtReflect.construct(CLS_SUPER_CASE, CLS_STRING, "1");
    assertNotNull(inst);
    assertEquals("1", inst.value);
  }

  @Test
  public void testDirectInjection_SubCase() throws Throwable {
    ReflectionCaseNoMagic inst;
    Constructor<? extends ReflectionCaseNoMagic> ctor;

    ctor = NO_MAGIC_SUBCLASS.getConstructor(CLS_LONG);
    inst = ctor.newInstance(1);
    assertNotNull(inst);
    assertEquals(2L, inst._long);// sub class adds 1 to values

    ctor = NO_MAGIC_SUBCLASS.getDeclaredConstructor(CLS_STRING);
    ctor.setAccessible(true);
    inst = ctor.newInstance("1");
    assertNotNull(inst);
    assertEquals("11", inst._String);
  }

  @Test
  public void testDirectConstruction_SubCase() throws Throwable {
    SuperCase inst;
    inst = GwtReflect.construct(CLS_SUB_CASE, CLS_LONG, 1L);
    assertNotNull(inst);
    assertEquals(2L, inst.value);

    inst = GwtReflect.construct(CLS_SUB_CASE, CLS_STRING, "1");
    assertNotNull(inst);
    assertEquals("11", inst.value);
  }

  @Test
  public void testConstructorVisibility() throws Throwable {
    final Constructor<?>[] ctors = SubCase.class.getConstructors();
    assertEquals(1, ctors.length); // Must not return super class constructors!!
    final SubCase inst = (SubCase) ctors[0].newInstance(1);
    assertEquals(2L, inst.value);
  }

  @Test
  public void testGwtReflectConstruct() throws Throwable {
    final SuperCase superCase = GwtReflect.construct(SuperCase.class, CLS_STRING, "1");
    assertEquals(superCase.value, "1");
  }

}
