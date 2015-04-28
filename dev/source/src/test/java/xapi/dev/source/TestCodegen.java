package xapi.dev.source;

import org.junit.Assert;
import org.junit.Test;

public class TestCodegen {

  @Test
  public void testGenericImports() {
    final SourceBuilder<Object> b = new SourceBuilder<Object>(
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
    final SourceBuilder<Object> b = new SourceBuilder<Object>(
      "public static abstract class Test");
    b.setPackage("xapi.test");
    b.getClassBuffer()
      .createMethod("public <T extends java.util.Date> void Test(java.lang.String t) {")
      .println("System.out.println(\"Hellow World\");")
      .createLocalClass("class InnerClass ")
      .createMethod("java.sql.Date innerMethod()")
      .returnValue("null")
      ;
    // We discard java.lang imports
    Assert.assertFalse(b.toString().contains("import java.lang.String;"));
    // We used java.util.Date as a fully qualified name first, so it should be imported
    Assert.assertTrue(b.toString().contains("import java.util.Date;"));
    Assert.assertTrue(b.toString().contains("<T extends Date>"));
    // We used java.sql.Date as a fqcn after java.util.Date, so it must NOT be imported
    Assert.assertFalse(b.toString().contains("import java.sql.Date;"));
  }

  @Test
  public void testMethodWithSimpleGeneric() {
    final SourceBuilder<Object> b = new SourceBuilder<Object>(
        "public static abstract class Test");
    b.getClassBuffer().createMethod(
        "public native <T> Class<T> magicClass(Class<T> ... cls)");
  }

  @Test
  public void testFieldWriter() {
    final SourceBuilder<Object> b = new SourceBuilder<Object>(
        "public static abstract class Test");
    b.setPackage("xapi.test");
    final FieldBuffer f = b.getClassBuffer()
    .createField("int", "theInt");
    f.addGetter(0);
    f.addSetter(0);
//    System.out.println(b);
  }

}
