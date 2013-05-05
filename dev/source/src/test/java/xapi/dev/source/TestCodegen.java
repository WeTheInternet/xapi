package xapi.dev.source;

import org.junit.Assert;
import org.junit.Test;

public class TestCodegen {

  @Test
  public void testGenericImports() {
    SourceBuilder<Object> b = new SourceBuilder<Object>(
      "public static abstract class xapi.Test");
    b.setPackage("xapi.test");
    b.getClassBuffer()
      .addGenerics("K","V extends java.util.Date")
      .addInterfaces("java.util.Iterator");
    Assert.assertTrue(b.toString().contains("import java.util.Date;"));
    Assert.assertTrue(b.toString().contains("import java.util.Iterator;"));
  }

  @Test
  public void testMethodWriter() {
    SourceBuilder<Object> b = new SourceBuilder<Object>(
      "public static abstract class Test");
    b.setPackage("xapi.test");
    b.getClassBuffer()
      .createMethod("public <T extends java.util.Date> void Test(java.lang.String t) {")
      .indentln("System.out.println(\"Hellow World\");")
      .createInnerClass("class InnerClass")
      .createMethod("void innerMethod()")
      ;
    // We discard java.lang imports
    Assert.assertFalse(b.toString().contains("import java.lang.String;"));
    Assert.assertTrue(b.toString().contains("import java.util.Date;"));
    Assert.assertTrue(b.toString().contains("<T extends Date>"));
  }

  @Test
  public void testMethodWithSimpleGeneric() {
    SourceBuilder<Object> b = new SourceBuilder<Object>(
        "public static abstract class Test");
    b.getClassBuffer().createMethod(
        "public native <T> Class<T> magicClass(Class<T> ... cls)");
  }

  @Test
  public void testFieldWriter() {
    SourceBuilder<Object> b = new SourceBuilder<Object>(
        "public static abstract class Test");
    b.setPackage("xapi.test");
    b.getClassBuffer()
    .createField("int", "theInt")
    .addGetter(0)
    .addSetter(0)
    ;
//    System.out.println(b);
  }

}
