package xapi.bytecode.impl;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import xapi.source.X_Source;
import xapi.source.api.*;

import java.io.Serializable;
import java.lang.annotation.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * A runtime-level annotation
 */
@Retention(RetentionPolicy.RUNTIME)
@interface Runtime{}
@Retention(RetentionPolicy.CLASS)
@interface Compiletime{}

/**
 * A test class containing a spattering of member types we need to handle.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@SuppressWarnings("serial")
@Runtime
@Compiletime
class OuterTestClass implements Serializable {

  public static class StaticInnerClass <T> implements Serializable{}

  class InnerClass <T extends Serializable> implements Serializable {

  }
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  private static @interface StaticInnerAnno{
    Class<?>[] value();
  }
  @Retention(RetentionPolicy.RUNTIME)
  @Target({})
  static @interface NoTargetAnno{
    Class<?>[] value() default {};
  }
  @Retention(RetentionPolicy.RUNTIME)
  @interface InnerAnno {
    StaticInnerAnno staticAnno() default @StaticInnerAnno({
      StaticInnerClass.class, InnerClass.class});
  }

  @InnerAnno(staticAnno=@StaticInnerAnno({}))
  private static void staticMethod(){}

  @StaticInnerAnno(OuterTestClass.class)
  public void instanceMethod(){}

  static String staticField = "withInitializer";
  static Class<?>[] staticClasses = {Class.class};

  @SuppressWarnings({ "unchecked", "unused" })
  private InnerClass<StaticInnerClass<InnerClass<OuterTestClass>>>[] instanceField =
      (InnerClass<StaticInnerClass<InnerClass<OuterTestClass>>>[])new InnerClass[0];

  OuterTestClass() {}

}



public class BytecodeAdapterServiceTest {

  private static BytecodeAdapterService service;
  @BeforeClass
  public static void extractClassFile() {
    service = new BytecodeAdapterService();
  }

  @Test
  public void testArrayParser() {
    IsClass clsArray = service.toClass("java.lang.Class[]");
    Assert.assertNotNull(clsArray);
  }

  @Test
  public void testAnnoNoArgs() {
    IsClass asClass = service.toClass(OuterTestClass.NoTargetAnno.class.getName());
    IsAnnotation anno = asClass.getAnnotation(Target.class.getName());

    IsAnnotationValue empty = anno.getValue(anno.getMethod("value"));
    Assert.assertTrue(empty.isArray());
    Assert.assertEquals(0, Array.getLength(empty.getRawValue()));
  }

  @Test
  public void testTestClass() {
    Class<?> cls = getTestClass();
    IsClass asClass = service.toClass(cls.getName());
    Assert.assertEquals(asClass.getPackage(), cls.getPackage().getName());
    Assert.assertEquals(asClass.getEnclosedName(), X_Source.classToEnclosedSourceName(cls));
    Assert.assertEquals(asClass.getModifier(), cls.getModifiers());
    testAnnos(cls.getDeclaredAnnotations(), asClass);
  }
  @Test
  public void testTestClass_Methods() {
    Class<?> cls = getTestClass();
    IsClass asClass = service.toClass(cls.getName());
    for (Method method : cls.getMethods()) {
      IsMethod imethod = asClass.getMethod(method.getName(), true, method.getParameterTypes());
      String testCase = imethod.getQualifiedName() +" != "+method.getName();
      Assert.assertNotNull(testCase, imethod);
      Assert.assertEquals(testCase, method.getName(), imethod.getName());
      Assert.assertEquals(testCase, method.getModifiers(), imethod.getModifier());
      Assert.assertEquals(testCase, imethod.getReturnType().getQualifiedName(), method.getReturnType().getCanonicalName());
      Assert.assertTrue(testCase, X_Source.typesEqual(imethod.getParameters(), method.getParameterTypes()));
      Assert.assertTrue(testCase, X_Source.typesEqual(imethod.getExceptions(), method.getExceptionTypes()));

      testAnnos(method.getDeclaredAnnotations(), imethod);
    }
  }

  @Test
  public void testTestClass_Fields() {
    Class<?> cls = getTestClass();
    IsClass asClass = service.toClass(cls.getName());
    for (Field field : cls.getFields()) {
      IsField ifield = asClass.getField(field.getName());
      Assert.assertNotNull(field.getName(), ifield);
      Assert.assertEquals(field.getName(), ifield.getName());
      Assert.assertEquals(field.getModifiers(), ifield.getModifier());

      testAnnos(field.getDeclaredAnnotations(), ifield);
    }
  }

  private void testAnnos(Annotation[] runtimeAnnotations, HasAnnotations hasAnnos) {
    for (Annotation anno : runtimeAnnotations) {
      IsAnnotation isAnno = hasAnnos.getAnnotation(anno.annotationType().getCanonicalName());
      Assert.assertNotNull("Missing annotation "+anno+" on "+hasAnnos,isAnno);
      Assert.assertEquals(anno.annotationType().getCanonicalName(), isAnno.getQualifiedName());
    }
    int cnt = 0;
    for (IsAnnotation anno : hasAnnos.getAnnotations()) {
      if (anno.isRuntime()) // classes only have runtime; we have runtime and compile time.
        cnt++;
    }
    Assert.assertEquals(runtimeAnnotations.length, cnt);
  }

  protected Class<?> getTestClass() {
    return OuterTestClass.class;
  }

  // Test something with type parameters!
}
