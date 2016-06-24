package xapi.javac.dev;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import xapi.fu.Out2;
import xapi.fu.Pointer;
import xapi.inject.X_Inject;
import xapi.javac.dev.api.CompilerService;
import xapi.javac.dev.model.CompilerSettings;
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
  @Ignore // Temporarily disable this test, as we wind up leaving temporary artifacts that intellij automatically adds to classpath...
  public void testSimpleCompile () throws Exception {
    // Compile a test resource and check if the GwtCreate plugin found our call to GWT.create

    CompilerService compiler = X_Inject.singleton(CompilerService.class);
    Out2<Integer, URL> result = compiler.compileFiles(testSettings(compiler),
        "test/Test.java", "ComplexTest.java");
    Assert.assertEquals("Javac failed", 0, result.out1().intValue());

    URLClassLoader cl = new URLClassLoader(new URL[]{result.out2()}, Thread.currentThread().getContextClassLoader());
    Pointer<Object> value = Pointer.pointer();
    Thread runIn = new Thread(()->{
      try {
        final Class<?> cls = cl.loadClass("dist.test.Test");
        Object o = cls.newInstance();
        Object test = cls.getMethod("finalFieldInitedByClassLiteral").invoke(o);
        value.in(test);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    runIn.setContextClassLoader(cl);
    runIn.start();
    runIn.join();
    Object test = value.out1();
//    assertThat(test).isNotNull();

  }

  private CompilerSettings testSettings(CompilerService compiler) {
    return compiler.defaultSettings()
        .setTest(true)
        .setClearGenerateDirectory(true);
  }
}
