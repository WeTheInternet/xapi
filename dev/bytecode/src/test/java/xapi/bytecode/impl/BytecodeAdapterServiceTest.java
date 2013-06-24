package xapi.bytecode.impl;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import xapi.source.X_Source;
import xapi.source.api.HasAnnotations;
import xapi.source.api.IsAnnotation;
import xapi.source.api.IsClass;
import xapi.source.api.IsField;
import xapi.source.api.IsMethod;

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
  public void testTestParser() {
    IsClass clsArray = service.toClass("java.lang.Class[]");
    
  }
  @Test
  public void testTestClass() {
    Class<?> cls = getTestClass();
    IsClass asClass = service.toClass(cls.getName());
    Assert.assertEquals(asClass.getPackage(), cls.getPackage().getName());
    Assert.assertEquals(asClass.getEnclosedName(), X_Source.classToEnclosedSourceName(cls));
    Assert.assertEquals(asClass.getModifier(), cls.getModifiers());
    testAnnos(cls.getDeclaredAnnotations(), asClass);
    
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
    for (Field field : cls.getFields()) {
      IsField ifield = asClass.getField(field.getName());
      Assert.assertNotNull(field.getName(), ifield);
      Assert.assertEquals(field.getName(), ifield.getDeclaredName());
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
  
}
