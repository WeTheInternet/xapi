package xapi.dev.denum;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import xapi.bytecode.ClassFile;
import xapi.dev.X_Dev;
import xapi.test.Assert;

public class DenumTest {

  private static enum TestEnum {
    Item0,
    Item1 {
      void test() {}
    },
    Item2() {
      void test() {
        String confuseLexer =
        		"public static enum NotAnEnum {" +
        		"\\\"enum{\\should, not, be, parsed;}" +
        		"}";
      }
    },
    Item3("enum DoNotParse{}"),
    Item4("\"\\\\\"enum NoParse{}") {
      @Override
      void test() {
        boolean pass;
        switch(TestEnum.Item0) {
        case Item0:
          pass = true;
          break;
        case Item1:
        default:
          pass = false;
        }
        Assert.assertTrue(pass);
      }
    }
    ;
    enum InnerEnum{
      Should, Be, Parsed
    }
    private TestEnum(){}
    private TestEnum(String var){}
    void test(){
      InnerEnum test = InnerEnum.Be;
      switch (test) {
        case Should:
        case Be:
        case Parsed:
      }
    }
  }
  
  private static Set<URL> uris;

  @BeforeClass
  public static void grabFiles() {
    // For faster tests, we'll only scan the test classpath
    URL self = DenumTest.class.getProtectionDomain().getCodeSource().getLocation();
    ClassLoader toScan = new URLClassLoader(new URL[]{self}, null);
    ClassLoader srcLoader = Thread.currentThread().getContextClassLoader();
    uris = new HashSet<URL>(); 
    try {
      for (ClassFile cls : X_Dev.findEnums(toScan)) {
        uris.add(srcLoader.getResource(cls.getResourceName()));
      }
    } finally {
      toScan = null;
    }
  }
  
  @Test
  public void testScanning() throws Exception{
    // Ensures our scanner is finding us
    URL self = DenumTest.class.getProtectionDomain().getCodeSource().getLocation();
    Assert.assertEquals(1, uris.size());
    Assert.assertEquals(uris.iterator().next(), 
        new URL(self,getClass().getName().replace('.', File.separatorChar)+".java"));
  }
  
  @Test
  public void testParsing() {
    // Pop open the source and parse out the enum classes.
    File file = new File(uris.iterator().next().getFile().replace("file:", ""));
    String content = X_Dev.readFile(file);
    EnumExtractor extractor = new EnumExtractor();
//    JavaLexer.visitClassFile(extractor, new EnumDefinition(), content, 0);
  }
  
}
