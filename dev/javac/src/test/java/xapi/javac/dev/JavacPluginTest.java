package xapi.javac.dev;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.junit.BeforeClass;
import org.junit.Test;

import xapi.test.Assert;
import xapi.util.X_Namespace;

import com.sun.tools.javac.Main;

public class JavacPluginTest {

  private static File root;
  
  @BeforeClass public static void install() throws IOException {
    root = new File(".").getCanonicalFile();
  }
  
  @Test
  public void testSimpleCompile () throws Exception {
    // Compile a test resource and check if the GwtCreate plugin found our call to GWT.create

    File resources = new File(root,"src/test/resources/");
    String input0 = new File(resources, "Test.java").getAbsolutePath();
    String input1 = new File(resources, "ComplexTest.java").getAbsolutePath();
    String jar = new File(root,"target/xapi-dev-javac-"+X_Namespace.XAPI_VERSION+".jar").getAbsolutePath();
    File gen = new File(root,"target/generated-sources/gwt");
    gen.mkdirs();
//    
    int result = Main.compile(new String[]{
//       "-verbose",
      "-d", gen.getAbsolutePath(),
//       "-s", resources.getAbsolutePath(),
       "-processorpath", 
       jar,
       "-Xplugin:GwtCreatePlugin", 
       input0//, input1
       
    }, new PrintWriter(System.out));
    System.out.println(gen+" exists? "+gen.exists());
    System.out.println(jar+" exists? "+new File(jar).exists());
    Assert.assertEquals("Javac failed", 0, result); 
  }
}
