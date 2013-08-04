package com.google.gwt.reflect.test;

import static com.google.gwt.reflect.test.AbstractAnnotation.MemberType.Annotation;
import static com.google.gwt.reflect.test.AbstractAnnotation.MemberType.Annotation_Array;
import static com.google.gwt.reflect.test.AbstractAnnotation.MemberType.Boolean;
import static com.google.gwt.reflect.test.AbstractAnnotation.MemberType.Boolean_Array;
import static com.google.gwt.reflect.test.AbstractAnnotation.MemberType.Class;
import static com.google.gwt.reflect.test.AbstractAnnotation.MemberType.Class_Array;
import static com.google.gwt.reflect.test.AbstractAnnotation.MemberType.Enum;
import static com.google.gwt.reflect.test.AbstractAnnotation.MemberType.Enum_Array;
import static com.google.gwt.reflect.test.AbstractAnnotation.MemberType.Int;
import static com.google.gwt.reflect.test.AbstractAnnotation.MemberType.Int_Array;
import static com.google.gwt.reflect.test.AbstractAnnotation.MemberType.Long;
import static com.google.gwt.reflect.test.AbstractAnnotation.MemberType.Long_Array;
import static com.google.gwt.reflect.test.AbstractAnnotation.MemberType.String;
import static com.google.gwt.reflect.test.AbstractAnnotation.MemberType.String_Array;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;

import org.junit.Test;

import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.reflect.client.strategy.ReflectionStrategy;
import com.google.gwt.reflect.test.annotations.ComplexAnnotation;
import com.google.gwt.reflect.test.annotations.SimpleAnnotation;

@ComplexAnnotation
@SuppressWarnings("all")
@ReflectionStrategy(annotationRetention=ReflectionStrategy.COMPILE|ReflectionStrategy.RUNTIME)
public class AnnotationTests extends AbstractReflectionTest{

  static class ValueImpl extends AbstractAnnotation implements SimpleAnnotation {

    public ValueImpl() {
      this("1");
    }
    public ValueImpl(String value) {
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
      Annotation singleAnnotation,
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
      var m = this.@com.google.gwt.reflect.test.AbstractAnnotation::memberMap;
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
      return this.@com.google.gwt.reflect.test.AbstractAnnotation::memberMap['singleBool'];
    }-*/;

    @Override
    public native int singleInt()
    /*-{
      return this.@com.google.gwt.reflect.test.AbstractAnnotation::memberMap['singleInt'];
    }-*/;

    @Override
    @UnsafeNativeLong
    public native long singleLong()
    /*-{
      return this.@com.google.gwt.reflect.test.AbstractAnnotation::memberMap['singleLong'];
    }-*/;

    @Override
    public native String singleString()
    /*-{
      return this.@com.google.gwt.reflect.test.AbstractAnnotation::memberMap['singleString'];
    }-*/;

    @Override
    public native ElementType singleEnum()
    /*-{
      return this.@com.google.gwt.reflect.test.AbstractAnnotation::memberMap['singleEnum'];
    }-*/;

    @Override
    public native SimpleAnnotation singleAnnotation()
    /*-{
      return this.@com.google.gwt.reflect.test.AbstractAnnotation::memberMap['singleAnnotation'];
    }-*/;

    @Override
    public native Class<?> singleClass()
    /*-{
      return this.@com.google.gwt.reflect.test.AbstractAnnotation::memberMap['singleClass'];
    }-*/;

    @Override
    public native boolean[] multiBool()
    /*-{
      return this.@com.google.gwt.reflect.test.AbstractAnnotation::memberMap['multiBool'];
    }-*/;

    @Override
    public native int[] multiInt()
    /*-{
      return this.@com.google.gwt.reflect.test.AbstractAnnotation::memberMap['multiInt'];
    }-*/;

    @Override
    @UnsafeNativeLong
    public native long[] multiLong()
    /*-{
      return this.@com.google.gwt.reflect.test.AbstractAnnotation::memberMap['multiLong'];
    }-*/;

    @Override
    public native String[] multiString()
    /*-{
      return this.@com.google.gwt.reflect.test.AbstractAnnotation::memberMap['multiString'];
    }-*/;

    @Override
    public native ElementType[] multiEnum()
    /*-{
      return this.@com.google.gwt.reflect.test.AbstractAnnotation::memberMap['multiEnum'];
    }-*/;

    @Override
    public native SimpleAnnotation[] multiAnnotation()
    /*-{
      return this.@com.google.gwt.reflect.test.AbstractAnnotation::memberMap['multiAnnotation'];
    }-*/;

    @Override
    public native Class<?>[] multiClass()
    /*-{
      return this.@com.google.gwt.reflect.test.AbstractAnnotation::memberMap['multiClass'];
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

  @Test
  public void testAnnotationMethods() {
    if (!GWT.isClient())
      return;// Don't let jvms try to load jsni; this @Test is gwt only
    TestCaseImpl impl1 = new TestCaseImpl();
    TestCaseImpl impl2 = new TestCaseImpl();
    assertEquals(impl1, impl2);
    assertEquals(impl1.toString(), impl2.toString());
    assertEquals(impl1.hashCode(), impl2.hashCode());
  }

  
  
}
