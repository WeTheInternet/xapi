package com.google.gwt.reflect.test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.reflect.client.GwtReflect;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.test.annotations.CompileRetention;
import com.google.gwt.reflect.test.annotations.RuntimeRetention;
import com.google.gwt.reflect.test.cases.ReflectionCaseHasAllAnnos;
import com.google.gwt.reflect.test.cases.ReflectionCaseSimple;

import static com.google.gwt.reflect.client.GwtReflect.*;

/**
 * @author James X. Nelson (james@wetheinter.net, @james)
 */
@ReflectionStrategy(magicSupertypes=false, keepCodeSource=true)
public class FieldTests extends AbstractReflectionTest {
  
  static final Class<ReflectionCaseSimple> c = magicClass(ReflectionCaseSimple.class);
  static final Class<Primitives> PRIMITIVE_CLASS = magicClass(Primitives.class);
  static final Class<Objects> OBJECTS_CLASS = magicClass(Objects.class);
  Primitives primitives;
  Objects objects;

  @Before 
  public void initObjects() throws InstantiationException, IllegalAccessException {
    primitives = PRIMITIVE_CLASS.newInstance();
    objects = OBJECTS_CLASS.newInstance();
  }
  
  public static class Primitives {
    public boolean z;
    public byte b;
    public char c;
    public short s;
    public int i;
    public long j;
    public float f;
    public double d;
  }    
  public static class Objects {
    public Object L;
    
    public final Object FINAL = null;
    
    public Boolean Z;
    public Byte B;
    public Character C;
    public Short S;
    public Integer I;
    public Long J;
    public Float F;
    public Double D;
  }
  
  public FieldTests() {}

  @Test(expected=NullPointerException.class)
  public void testObjectNullAccess() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("L");
    f.get(null);
  }
  
  @Test
  public void testBooleanLegalUse() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("z");
    assertFalse(f.getBoolean(primitives));
    assertFalse((Boolean)f.get(primitives));
    f.set(primitives, true);
    assertTrue(f.getBoolean(primitives));
    assertTrue((Boolean)f.get(primitives));
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testBooleanIllegalGet() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getDeclaredField("z");
    assertEquals(1, f.getInt(primitives));
  }

  @Test(expected=IllegalArgumentException.class)
  public void testBooleanIllegalSet() throws Exception {
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("z");
    assertFalse(f.getBoolean(primitives));
    assertFalse((Boolean)f.get(primitives));
    
    f.set(primitives, 1);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testBooleanPrimitiveNullSet() throws Exception {    
    assertNotNull(primitives);
    Field f = PRIMITIVE_CLASS.getField("z");
    
    assertFalse(f.getBoolean(primitives));
    assertFalse((Boolean)f.get(primitives));
    
    f.set(primitives, (Boolean)null);
  }

  @Test
  public void testBooleanObjectNullSet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("Z");
    assertNull(f.get(objects));
    objects.Z = true;
    assertNotNull(f.get(objects));
    f.set(objects, null);
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testBooleanObjectNullGet() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("Z");
    assertNull(f.get(objects));
    assertFalse(f.getBoolean(objects));
  }

  @Test(expected=IllegalAccessException.class)
  public void testSetFinal() throws Exception {
    assertNotNull(objects);
    Field f = OBJECTS_CLASS.getField("FINAL");
    f.set(objects, objects);
  }

}
