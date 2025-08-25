package xapi.shell;

import java.io.File;
import java.util.concurrent.CountDownLatch;

import org.junit.BeforeClass;
import org.junit.Test;

import xapi.file.X_File;
import xapi.inject.impl.JavaInjector;
import xapi.io.api.StringReader;
import xapi.log.X_Log;
import xapi.shell.api.ShellCommand;
import xapi.shell.api.ShellSession;
import xapi.test.Assert;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.constants.X_Namespace;

public class ShellTesting {

  private static final String success = "succ\ness";
  private static final int exitStatus = 111;

  @BeforeClass
  public static void prepare() {
    System.setProperty(X_Namespace.PROPERTY_MULTITHREADED, "10");
    JavaInjector.instance(ShellCommand.class);
  }

  public static void main(String[] args) {
    System.out.print(success);
    System.exit(exitStatus);// Test custom exit status
  }

  @Test(timeout=150_000)
  public void testInvokeJavaMain() {
    Moment start = X_Time.now();
    String[] classpath = System.getProperty("java.class.path").split(File.pathSeparator);
    StringReader reader = new StringReader();
    ShellSession result = X_Shell.launchJava(ShellTesting.class, classpath, new String[]{"-Xmx128M"}, new String[0]);
    result.stdOut(reader);
    X_Log.debug(ShellTesting.class, "Bootstrap shell", X_Time.difference(start));
    try {
      reader.waitToEnd(14000, 0);
    } catch (InterruptedException e) {
      Assert.fail("Interrupted while waiting for process to end");
    }
    Assert.assertEquals(success, reader.toString());
    Assert.assertEquals(exitStatus, result.join());
    X_Log.trace(ShellTesting.class, "Process Runtime", X_Time.difference(start));
  }

  @Test(timeout=100_000)
  public void testShell() throws Throwable {

    final CountDownLatch job = new CountDownLatch(1);
    final Throwable[] error = new Throwable[1];
    X_Time.runLater(new Runnable() {
      @Override
      public void run() {
        try{
          runTest();
        } catch (Throwable e){
          X_Log.error("Error testing shell commands", e);
          error[0] = e;
        } finally {
          job.countDown();
        }
      }
    });
    job.await();
    if (error[0] != null) {
      throw error[0];
    }
  }
  private void runTest() throws Exception {
    final StringReader d = new StringReader();
    final StringReader e = new StringReader();
    final boolean[] success = new boolean[1];
    final ShellSession shell = X_Shell.newService().newCommand(
        "sh"
        ,"-ac",X_File.unzippedResourcePath("xapi/repl.sh", null)
        )
        .run(t -> {
          synchronized (success){
            success[0] = true;
          }
        }, null, d, e);

    shell.stdIn("echo \"test success\"");
    shell.stdIn("exit");
    d.waitToEnd();
    Assert.assertEquals("test success", d.toString().trim());
    synchronized (success) {
      Assert.assertTrue(success[0]);
    }

//    X_Log.info(shell.stdIn("javac ~/XApi.java -d /tmp && cd /tmp; java XApi"));
  }

}
