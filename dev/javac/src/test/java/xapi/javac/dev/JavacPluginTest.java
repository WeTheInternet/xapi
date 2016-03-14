package xapi.javac.dev;

import com.sun.tools.javac.Main;
import org.junit.BeforeClass;
import org.junit.Test;
import xapi.log.X_Log;
import xapi.test.Assert;
import xapi.util.X_Namespace;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class JavacPluginTest {

  private static File root;

  @BeforeClass public static void install() throws IOException {
    root = new File(".").getCanonicalFile();
  }

  @Test
  public void testSimpleCompile () throws Exception {
    // Compile a test resource and check if the GwtCreate plugin found our call to GWT.create

    File resources = new File(root,"src/test/resources/");
    File testClasses = new File(root,"target/test-classes");
    String input0 = new File(resources, "Test.java").getAbsolutePath();
    String input1 = new File(resources, "ComplexTest.java").getAbsolutePath();
    String jar = new File(root,"target/xapi-dev-javac-"+X_Namespace.XAPI_VERSION+".jar").getAbsolutePath();
    File gen = new File(root,"target/generated-sources/gwt");
    gen.mkdirs();

//
    // This is the old-school way of running javac;
    // The JavacService uses the compiler directly.
    int result = Main.compile(new String[]{
//       "-verbose",
      "-d", testClasses.getAbsolutePath(), // output directory
       "-sourcepath", resources.getAbsolutePath(), // source files we want to compile
        "-s", gen.getAbsolutePath(), // generated source output directory

//       "-processorpath",
//       jar,

       "-Xplugin:GwtCreatePlugin",
       "-Xplugin:XapiCompilerPlugin",

        "-Xprefer:newer", // choose the newer of source or class files for loaded types
//       "-Xprefer:source",

//       "-implicit:class", // always generate classes for all source.
       "-implicit:none", // do not generate classes for implicitly loaded types

        "-g", // generate all debugging information
       input0
       ,
       input1

    }, new PrintWriter(System.out));
    X_Log.info(getClass(), gen," exists? "+gen.exists());
    X_Log.info(getClass(), jar," exists? "+new File(jar).exists());
    Assert.assertEquals("Javac failed", 0, result);

  }
}
