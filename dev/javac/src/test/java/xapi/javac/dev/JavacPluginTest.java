package xapi.javac.dev;

import org.junit.BeforeClass;
import org.junit.Test;
import xapi.fu.Out2;
import xapi.inject.X_Inject;
import xapi.javac.dev.api.CompilerService;
import xapi.test.Assert;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

public class JavacPluginTest {

  private static File root;

  @BeforeClass public static void install() throws IOException {
    root = new File(".").getCanonicalFile();
  }

  @Test
  public void testSimpleCompile () throws Exception {
    // Compile a test resource and check if the GwtCreate plugin found our call to GWT.create

    CompilerService compiler = X_Inject.singleton(CompilerService.class);
    Out2<Integer, URL> result = compiler.compileFiles(
        compiler.defaultSettings().setTest(true).setClearGenerateDirectory(true),
        "test/Test.java", "ComplexTest.java");
    Assert.assertEquals("Javac failed", 0, result.out1().intValue());

    URLClassLoader cl = new URLClassLoader(new URL[]{result.out2()}, Thread.currentThread().getContextClassLoader());
    Thread runIn = new Thread(()->{
      try {

        final Class<?> cls = cl.loadClass("dist.test.Test");
        Object o = cls.newInstance();
        cls.getMethod("finalFieldInitedByClassLiteral").invoke(o);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    runIn.setContextClassLoader(cl);
    runIn.start();
    runIn.join();

  }
}
