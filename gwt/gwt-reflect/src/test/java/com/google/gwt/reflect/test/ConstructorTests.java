package com.google.gwt.reflect.test;

import java.lang.reflect.Constructor;

import org.junit.Test;

import com.google.gwt.reflect.client.GwtReflect;

public class ConstructorTests extends AbstractReflectionTest {

  private static final Class<? extends SuperCase>
    CLS_SUPER_CASE = SuperCase.class,
    // We purposely want to erase the SubCase to SuperCase, 
    // to make sure the field type does not influence our lookup.
    CLS_SUB_CASE = SubCase.class;
  
  @SuppressWarnings("rawtypes")
  private static final Class[]
    CLS_LONG = new Class[]{long.class},
    CLS_STRING = new Class[]{String.class};
  
  @SuppressWarnings("unused")// it's a reflection test, after all
  private static class SuperCase {
    Object value;
    private SuperCase() {
      value = this;
    }
    
    private SuperCase(String s) {
      value = s;
    }
    
    public SuperCase(long j) {
      value = j;
    }
    
    // Here to make sure sub classes can't find super class public constructors
    public SuperCase(Enum<?> e) {
      value = e;
    }
    
  }
  
  @SuppressWarnings("unused")
  private static class SubCase extends SuperCase {
    public SubCase(long l) {
      super(l+1);// to differentiate between super and sub class
    }
    private SubCase(String s) {
      super(s+"1");
    }
  }
  
  @Test
  public void testSimpleCtor() throws Throwable {
    SuperCase inst;
    Constructor<? extends SuperCase> ctor;
    
    ctor = CLS_SUPER_CASE.getConstructor(CLS_LONG);
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
  public void testMagicsSubCase() throws Throwable {
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
    Constructor<?>[] ctors = SubCase.class.getConstructors();
    assertEquals(1, ctors.length);
    for (Constructor<?> ctor : ctors) {
      SubCase inst = (SubCase) ctor.newInstance(1);
      assertEquals(2L, inst.value);
    }
  }
  
}
