package xapi.source.read;

import java.lang.reflect.Modifier;

import org.junit.Assert;
import org.junit.Test;

import xapi.source.read.JavaVisitor.AnnotationMemberVisitor;
import xapi.source.read.JavaVisitor.AnnotationVisitor;
import xapi.source.read.JavaVisitor.MethodVisitor;
import xapi.source.read.JavaVisitor.ParameterVisitor;
import xapi.source.read.JavaVisitor.TypeData;

public class JavaLexerTest {

  @Test
  public void testAnnotationLexer() {
    final short[] success = new short[1];
    final String annoBody =
        "name=\"one \\\"\\\"two\\\"\\\" three\\\\\", " +
            "{1, 2.0, false}, values={1, \"string\", com.test.Class.class}";

    JavaLexer.visitAnnotation(new AnnotationVisitor<Void>(){

      @Override
      public AnnotationMemberVisitor<Void> visitAnnotation(String annoName, String annoContent,
          Void receiver) {
        success[0] = 1;
        Assert.assertEquals(annoName, "java.lang.Annotation");
        Assert.assertEquals(annoContent, annoBody);
        return null;

      }

    }, null, "@java.lang.Annotation(" + annoBody + ")" , 0);

    Assert.assertEquals(success[0], 1);
  }

  @Test
  public void testTypeLexer() {

    String type = "java.lang.Class";

    TypeData data = JavaLexer.extractType(type, 0);
    Assert.assertEquals("java.lang", data.pkgName);
    Assert.assertEquals("Class", data.clsName);
    Assert.assertEquals("Class", data.simpleName);
    Assert.assertEquals(type, data.toString());

    String generics = "<SomeGenerics<With, Generics, In<Them>>>";
    type = "com.foo.Outer.Inner" + generics + " [] [][ ]";
    data = JavaLexer.extractType(type, 0);
    Assert.assertEquals("com.foo", data.pkgName);
    Assert.assertEquals("Outer.Inner", data.clsName);
    Assert.assertEquals("Inner", data.simpleName);
    Assert.assertEquals(3, data.arrayDepth);
    Assert.assertEquals(generics, data.generics);

  }
  @Test
  public void testMethodLexer() {
    final String methodGeneric = "<Complex, Generic extends Signature & More<Stuff>>";
    final String modifiers = "public static final";
    final String returnType = "Type";
    final String returnGeneric = "<With<Generics>>";
    final String methodName = "methodName";
    final String paramGeneric = "<? extends Generic<int[]>>";
    String params = "String[] i, java . lang . Class" +paramGeneric+ " cls";
    String exceptions = "java.lang.RuntimeException, ClassCastException  ";

    String method = modifiers + " "+methodGeneric +" "+
        returnType+returnGeneric+"[][] " + methodName+
        "(" + params+") throws "+exceptions;

    final int[] success = new int[8];

    JavaLexer.visitMethodSignature(new MethodVisitor<Void>() {

      @Override
      public AnnotationMemberVisitor<Void> visitAnnotation(String annoName, String annoBody,
          Void receiver) {
        Assert.fail("No annotations");
        return null;
      }

      @Override
      public void visitJavadoc(String javadoc, Void receiver) {
        Assert.fail("No javadoc");
      }

      @Override
      public void visitModifier(int modifier, Void receiver) {
        switch(modifier) {
        case Modifier.PUBLIC:
        case Modifier.STATIC:
        case Modifier.FINAL:
          success[0] = success[0] | modifier;
          break;
        default:
          Assert.fail("Illegal modifier: "+Integer.toHexString(modifier));
        }
      }

      @Override
      public void visitGeneric(String generic, Void receiver) {
        Assert.assertEquals(generic, methodGeneric);
        success[1] = 1;
      }

      @Override
      public void visitReturnType(TypeData returnedType,
          Void receiver) {
        Assert.assertEquals(returnedType.clsName, returnType);
        Assert.assertEquals(returnedType.generics, returnGeneric);
        Assert.assertEquals(returnedType.arrayDepth, 2);
        success[2] = 1;
      }

      @Override
      public void visitName(String name, Void receiver) {
        Assert.assertEquals(name, methodName);
        success[3] = 1;
      }

      @Override
      public ParameterVisitor<Void> visitParameter() {
        return new ParameterVisitor<Void>() {

          @Override
          public AnnotationMemberVisitor<Void> visitAnnotation(String annoName, String annoBody,
              Void receiver) {
            return null;
          }

          @Override
          public void visitModifier(int modifier, Void receiver) {

          }

          @Override
          public void visitType(TypeData type, String name, boolean varargs, Void receiver) {
            if ("i".equals(name)) {
              success[4] = 1;
              Assert.assertEquals("String", type.clsName);
              Assert.assertEquals(1, type.arrayDepth);
            } else if ("cls".equals(name)){
              success[5] = 1;
              Assert.assertEquals("java.lang", type.pkgName);
              Assert.assertEquals("Class", type.clsName);
              Assert.assertEquals(0, type.arrayDepth);
              Assert.assertEquals(paramGeneric, type.generics);
            } else {
              Assert.fail("Unrecognized name: "+name+"; type: "+type);
            }
          }

        };
      }

      @Override
      public void visitException(String type, Void receiver) {
        if ("java.lang.RuntimeException".equals(type)) {
          success[6] = 1;
        } else if ("ClassCastException".equals(type)) {
          success[7] = 1;
        } else {
          Assert.fail("Unrecognized exception type: "+type);
        }
      }
    }, null, method, 0);

    // Make sure everything got called.
    Assert.assertArrayEquals(success, new int[]{
        Modifier.PUBLIC|Modifier.STATIC|Modifier.FINAL
        ,1,1,1, 1,1,1,1 });
  }
}
