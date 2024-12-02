package xapi.javac.dev;

import org.junit.BeforeClass;
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

import com.google.gwt.core.server.ServerGwtBridge;

import static org.assertj.core.api.Assertions.assertThat;

public class JavacPluginTest {

  private static File root;

  @BeforeClass public static void install() throws IOException {
    root = new File(".").getCanonicalFile();
    // dirty hack to let this test of a half-finished feature produce less noise.
    ServerGwtBridge.getInstance();
  }

  @Test
  public void testSimpleCompile () throws Exception {
    // Compile a test resource and check if the GwtCreate plugin found our call to GWT.create

    CompilerService compiler = X_Inject.singleton(CompilerService.class);

    final CompilerSettings settings = testSettings(compiler);
    try {

      Out2<Integer, URL> result = compiler.compileFiles(settings,
          "test/Test.java", "ComplexTest.java");
      Assert.assertEquals("Javac failed", 0, result.out1().intValue());

      URLClassLoader cl = new URLClassLoader(new URL[]{result.out2()}, Thread.currentThread().getContextClassLoader());
      Pointer<Object> value = Pointer.pointer();
      Thread runIn = new Thread(()->{
        try {
          final Class<?> cls = cl.loadClass("test.Test");
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
      System.out.println(test);
      assertThat(test).isNotNull();
    } finally {
      // cleanup after ourselves.  set a break point here to inspect files before they are deleted.
      settings.resetGenerateDirectory();
    }

  }

  private CompilerSettings testSettings(CompilerService compiler) {
    return compiler.defaultSettings()
        .setTest(true)
        .setUseRuntimeClasspath(true)
        .setClearGenerateDirectory(true);
  }
}
