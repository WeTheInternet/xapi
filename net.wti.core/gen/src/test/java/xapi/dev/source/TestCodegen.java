package xapi.dev.source;

import org.junit.Assert;
import org.junit.Test;

public class TestCodegen {

  @Test
  public void testGenericImports() {
    final SourceBuilder<Object> b = new SourceBuilder<Object>(
      "public static abstract class Test");
    b.setPackage("xapi.test");
    b.getClassBuffer()
      .addGenerics("K","V extends java.util.Date, ? extends java.lang.String, ? super xapi.test.Thing")
      .addInterfaces("java.util.Iterator");
    String src = b.toSource();
    Assert.assertTrue("Must contain import java.util.Date;\n" + src,
        src.contains("import java.util.Date;"));
    Assert.assertTrue("Must contain import java.util.Iterator;\n" + src
        , b.toString().contains("import java.util.Iterator;"));
  }
  @Test
  public void testMethodWriter() {
    final SourceBuilder<Object> b = new SourceBuilder<Object>(
      "public static abstract class Test");
    b.setPackage("xapi.test");
    b.getClassBuffer()
      .createMethod("public <T extends java.util.Date, V extends xapi.test.Type[]> void Test(java.lang.String t) {")
      .println("System.out.println(\"Hellow World\");")
      .createLocalClass("class InnerClass ")
      .createMethod("java.sql.Date innerMethod()")
      .returnValue("null")
      ;
    final String src = b.toString();
    System.out.println(src);
    // We discard java.lang imports
    Assert.assertFalse(src.contains("import java.lang.String;"));
    // We used java.util.Date as a fully qualified name first, so it should be imported
    Assert.assertTrue(src.contains("import java.util.Date;"));
    Assert.assertTrue(src.contains("<T extends Date, V extends Type[]>"));
    // We used java.sql.Date as a fqcn after java.util.Date, so it must NOT be imported
    Assert.assertFalse(src.contains("import java.sql.Date;"));
  }

  @Test
  public void testImportCleanup() {
    String imported = new SourceBuilder<>().addImport("java.util.concurrent.Callable");
    Assert.assertEquals("Callable", imported);

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
