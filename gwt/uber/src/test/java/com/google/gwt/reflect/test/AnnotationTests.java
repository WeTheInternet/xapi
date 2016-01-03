package com.google.gwt.reflect.test;

import org.junit.Test;
import xapi.gwt.junit.api.JUnitExecution;

import static com.google.gwt.reflect.shared.GwtReflect.magicClass;
import static com.google.gwt.reflect.test.annotations.AbstractAnnotation.MemberType.Annotation;
import static com.google.gwt.reflect.test.annotations.AbstractAnnotation.MemberType.Annotation_Array;
import static com.google.gwt.reflect.test.annotations.AbstractAnnotation.MemberType.Boolean;
import static com.google.gwt.reflect.test.annotations.AbstractAnnotation.MemberType.Boolean_Array;
import static com.google.gwt.reflect.test.annotations.AbstractAnnotation.MemberType.Class;
import static com.google.gwt.reflect.test.annotations.AbstractAnnotation.MemberType.Class_Array;
import static com.google.gwt.reflect.test.annotations.AbstractAnnotation.MemberType.Enum;
import static com.google.gwt.reflect.test.annotations.AbstractAnnotation.MemberType.Enum_Array;
import static com.google.gwt.reflect.test.annotations.AbstractAnnotation.MemberType.Int;
import static com.google.gwt.reflect.test.annotations.AbstractAnnotation.MemberType.Int_Array;
import static com.google.gwt.reflect.test.annotations.AbstractAnnotation.MemberType.Long;
import static com.google.gwt.reflect.test.annotations.AbstractAnnotation.MemberType.Long_Array;
import static com.google.gwt.reflect.test.annotations.AbstractAnnotation.MemberType.String;
import static com.google.gwt.reflect.test.annotations.AbstractAnnotation.MemberType.String_Array;

import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.reflect.client.strategy.GwtRetention;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.test.annotations.AbstractAnnotation;
import com.google.gwt.reflect.test.annotations.CompileRetention;
import com.google.gwt.reflect.test.annotations.ComplexAnnotation;
import com.google.gwt.reflect.test.annotations.InheritedAnnotation;
import com.google.gwt.reflect.test.annotations.RuntimeRetention;
import com.google.gwt.reflect.test.annotations.SimpleAnnotation;
import com.google.gwt.reflect.test.annotations.UninheritedAnnotation;
import com.google.gwt.reflect.test.cases.ReflectionCaseHasAllAnnos;
import com.google.gwt.reflect.test.cases.ReflectionCaseSimple;
import com.google.gwt.reflect.test.cases.ReflectionCaseSubclass;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@ComplexAnnotation
@SuppressWarnings("all")
@ReflectionStrategy(
    // Keep all annotations on this type
    annotationRetention=ReflectionStrategy.ALL_ANNOTATIONS,
    // Keep all annotations on this type's members
    memberRetention=@GwtRetention(
      annotationRetention=ReflectionStrategy.ALL_ANNOTATIONS
    )
)
public class AnnotationTests extends AbstractReflectionTest{

  public JUnitExecution exe;

  static class ValueImpl extends AbstractAnnotation implements SimpleAnnotation {

    public ValueImpl() {
      this("1");
    }
    public ValueImpl(final String value) {
      setValue("value", value);
    }

    @Override
    public Class<? extends Annotation> annotationType() {
      return SimpleAnnotation.class;
    }

    @Override
    public String value() {
      return getValue("value");
    }

    @Override
    protected String[] members() {
      return new String[] {"value"};
    }

    @Override
    protected MemberType[] memberTypes() {
      return new MemberType[] {String};
    }
  }

  static class TestCaseImpl extends AbstractAnnotation implements ComplexAnnotation {
    private static final String[] members = new String[] {
      "singleBool",
      "singleInt",
      "singleLong",
      "singleString",
      "singleEnum",
      "singleAnnotation",
      "singleClass",
      "multiBool",
      "multiInt",
      "multiLong",
      "multiString",
      "multiEnum",
      "multiAnnotation",
      "multiClass"
    };
    private static final MemberType[] types = new MemberType[] {
      Boolean,
      Int,
      Long,
      String,
      Enum,
      Annotation,
      Class,
      Boolean_Array,
      Int_Array,
      Long_Array,
      String_Array,
      Enum_Array,
      Annotation_Array,
      Class_Array
    };

    public TestCaseImpl() {

      set(
        true,
        1,
        2,
        "3",
        ElementType.ANNOTATION_TYPE,
        new ValueImpl(),
        ElementType.class,
        new boolean[]{true, false, true},
        new int[] {1, 3, 2},
        new long[] {2, 4, 3},
        new String[] {"3", "0", "a"},
        new ElementType[] {ElementType.CONSTRUCTOR, ElementType.ANNOTATION_TYPE},
        new SimpleAnnotation[] {new ValueImpl(), new ValueImpl("2")},
        new Class<?>[] {ElementType.class, SimpleAnnotation.class}
        );
    }

    @UnsafeNativeLong
    public native void set(
      boolean singleBool,
      int singleInt,
      long singleLong,
      String singleString,
      Enum<?> singleEnum,
      SimpleAnnotation singleAnnotation,
      Class<?> singleClass,
      boolean[] multiBool,
      int[] multiInt,
      long[] multiLong,
      String[] multiString,
      Enum<?>[] multiEnum,
      Annotation[] multiAnnotation,
      Class<?>[] multiClass
      )
    /*-{
      var m = this.@com.google.gwt.reflect.test.annotations.AbstractAnnotation::memberMap;
      m['singleBool'] = singleBool;
      m['singleInt'] = singleInt;
      m['singleLong'] = singleLong;
      m['singleString'] = singleString;
      m['singleEnum'] = singleEnum;
      m['singleAnnotation'] = singleAnnotation;
      m['singleClass'] = singleClass;
      m['multiBool'] = multiBool;
      m['multiInt'] = multiInt;
      m['multiLong'] = multiLong;
      m['multiString'] = multiString;
      m['multiEnum'] = multiEnum;
      m['multiAnnotation'] = multiAnnotation;
      m['multiClass'] = multiClass;
    }-*/;

    @Override
    public native boolean singleBool()
    /*-{
      return this.@com.google.gwt.reflect.test.annotations.AbstractAnnotation::memberMap['singleBool'];
    }-*/;

    @Override
    public native int singleInt()
    /*-{
      return this.@com.google.gwt.reflect.test.annotations.AbstractAnnotation::memberMap['singleInt'];
    }-*/;

    @Override
    @UnsafeNativeLong
    public native long singleLong()
    /*-{
      return this.@com.google.gwt.reflect.test.annotations.AbstractAnnotation::memberMap['singleLong'];
    }-*/;

    @Override
    public native String singleString()
    /*-{
      return this.@com.google.gwt.reflect.test.annotations.AbstractAnnotation::memberMap['singleString'];
    }-*/;

    @Override
    public native ElementType singleEnum()
    /*-{
      return this.@com.google.gwt.reflect.test.annotations.AbstractAnnotation::memberMap['singleEnum'];
    }-*/;

    @Override
    public native SimpleAnnotation singleAnnotation()
    /*-{
      return this.@com.google.gwt.reflect.test.annotations.AbstractAnnotation::memberMap['singleAnnotation'];
    }-*/;

    @Override
    public native Class<?> singleClass()
    /*-{
      return this.@com.google.gwt.reflect.test.annotations.AbstractAnnotation::memberMap['singleClass'];
    }-*/;

    @Override
    public native boolean[] multiBool()
    /*-{
      return this.@com.google.gwt.reflect.test.annotations.AbstractAnnotation::memberMap['multiBool'];
    }-*/;

    @Override
    public native int[] multiInt()
    /*-{
      return this.@com.google.gwt.reflect.test.annotations.AbstractAnnotation::memberMap['multiInt'];
    }-*/;

    @Override
    @UnsafeNativeLong
    public native long[] multiLong()
    /*-{
      return this.@com.google.gwt.reflect.test.annotations.AbstractAnnotation::memberMap['multiLong'];
    }-*/;

    @Override
    public native String[] multiString()
    /*-{
      return this.@com.google.gwt.reflect.test.annotations.AbstractAnnotation::memberMap['multiString'];
    }-*/;

    @Override
    public native ElementType[] multiEnum()
    /*-{
      return this.@com.google.gwt.reflect.test.annotations.AbstractAnnotation::memberMap['multiEnum'];
    }-*/;

    @Override
    public native SimpleAnnotation[] multiAnnotation()
    /*-{
      return this.@com.google.gwt.reflect.test.annotations.AbstractAnnotation::memberMap['multiAnnotation'];
    }-*/;

    @Override
    public native Class<?>[] multiClass()
    /*-{
      return this.@com.google.gwt.reflect.test.annotations.AbstractAnnotation::memberMap['multiClass'];
    }-*/;

    @Override
    public Class<? extends Annotation> annotationType() {
      return ComplexAnnotation.class;
    }

    @Override
    protected String[] members() {
      return members;
    }

    @Override
    protected MemberType[] memberTypes() {
      return types;
    }

  }

  @SimpleAnnotation
  Object field;

  @SimpleAnnotation
  void method(){}

  @Test
  public void testAnnotationMethods() {
    if (!GWT.isClient())
     {
      return;// Don't let jvms try to load jsni; this @Test is gwt only
    }
    final TestCaseImpl impl1 = new TestCaseImpl();
    final TestCaseImpl impl2 = new TestCaseImpl();
    assertEquals(impl1, impl2);
    assertEquals(impl1.toString(), impl2.toString());
    assertEquals(impl1.hashCode(), impl2.hashCode());
  }

  @Test
  public void testSimpleReflection() throws Exception {
    final Class<ReflectionCaseSimple> c = ReflectionCaseSimple.class;
    final ReflectionCaseSimple inst = testNewInstance(magicClass(c));
    final ReflectionCaseSimple anon = new ReflectionCaseSimple() {};
    testAssignable(inst, anon);

    testHasNoArgDeclaredMethods(c, "privatePrimitive", "privateObject", "publicPrimitive", "publicObject");
    testHasNoArgPublicMethods(c, "publicPrimitive", "publicObject", "hashCode", "toString");
    testCantAccessNonPublicMethods(c, "privatePrimitive", "privateObject");
    testCantAccessNonDeclaredMethods(c, "hashCode", "toString");
  }

  @Test
  public void testSelfEquals() {
    final ComplexAnnotation self = AnnotationTests.class.getAnnotation(ComplexAnnotation.class);
    assertEquals(self, self);
  }

  @Test
  @ComplexAnnotation
  public void testOtherEquals() throws Throwable {
    final ComplexAnnotation onType = AnnotationTests.class.getAnnotation(ComplexAnnotation.class);
    final Method method = AnnotationTests.class.getMethod("testOtherEquals");
    final ComplexAnnotation onMethod = method.getAnnotation(ComplexAnnotation.class);
    assertTrue(onType.equals(onMethod));
    log(onType.toString());
  }

  /**
   * @param onType
   */
  private native void log(String onType)
  /*-{
     $wnd.console.log(onType);
  }-*/;

  @Test
  public void testAnnotationsKeepAll() throws Exception {
    final Class<?> testCase = magicClass(ReflectionCaseHasAllAnnos.class);
    final Field field = testCase.getDeclaredField("field");
    final Method method = testCase.getDeclaredMethod("method", Long.class);
    final Constructor<?> ctor = testCase.getDeclaredConstructor(long.class);
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

  @Test
  public void testAnnotationOnField() throws Throwable {
    final Field f = AnnotationTests.class.getDeclaredField("field");
    final SimpleAnnotation anno = f.getAnnotation(SimpleAnnotation.class);
    assertNotNull(anno);
  }

  @Test
  public void testInheritsAnnotations() throws Throwable {
    final InheritedAnnotation inherited = ReflectionCaseHasAllAnnos.class.getAnnotation(InheritedAnnotation.class);
    final UninheritedAnnotation uninherited = ReflectionCaseHasAllAnnos.class.getAnnotation(UninheritedAnnotation.class);
    assertNotNull(inherited);
    assertNull(uninherited);
  }

  @Test
  public void testInheritsAnnotationsFilter() throws Throwable {
    // In order to use a class reference here and bypass the injector, which will ignore any sort
    // of reflection strategy restrictions, we must ensure that our class reference is not tracable
    // to a constant literal expression.  Thus, we obscure it with an opaque conditional guaranteed to be false
    final Class<ReflectionCaseSubclass> cls = Math.random() == -1 ? null : ReflectionCaseSubclass.class;
    final InheritedAnnotation inherited = cls.getAnnotation(InheritedAnnotation.class);
    final UninheritedAnnotation uninherited = cls.getAnnotation(UninheritedAnnotation.class);
    assertNull(inherited);
    assertNull(uninherited);
  }

  private void assertHasAnno(final Class<?> cls, final Annotation[] annos, final Class<? extends Annotation> annoClass) {
    for (final Annotation anno : annos) {
      if (anno.annotationType() == annoClass) {
        return;
      }
    }
    fail(cls.getName()+" did not have required annotation "+annoClass);
  }

  private void testCantAccessNonPublicMethods(final Class<?> c, final String ... methods) {
    for (final String method : methods) {
      try {
        c.getMethod(method);
        fail("Could erroneously access non-public method "+method+" in "+c.getName());
      } catch (final NoSuchMethodException e) {}
    }
  }

  private void testCantAccessNonDeclaredMethods(final Class<?> c, final String ... methods) {
    for (final String method : methods) {
      try {
        c.getDeclaredMethod(method);
        fail("Could erroneously access non-declared method "+method+" in "+c.getName());
      } catch (final NoSuchMethodException e) {}
    }
  }

  private void testHasNoArgDeclaredMethods(final Class<?> c, final String ... methods) throws Exception{
    for (final String method : methods) {
      assertNotNull(c.getDeclaredMethod(method));
    }
  }
    private void testHasNoArgPublicMethods(final Class<?> c, final String ... methods) throws Exception{
      for (final String method : methods) {
        assertNotNull(c.getMethod(method));
      }
  }

  private void testAssignable(final Object inst, final Object anon) {
    assertTrue(inst.getClass().isAssignableFrom(anon.getClass()));
    assertFalse(anon.getClass().isAssignableFrom(inst.getClass()));
  }

  private <T> T testNewInstance(final Class<T> c) throws Exception {
    final T newInst = c.newInstance();
    assertNotNull(c.getName()+" returned null instead of a new instance", newInst);
    assertTrue(c.isAssignableFrom(newInst.getClass()));
    return newInst;
  }


}
