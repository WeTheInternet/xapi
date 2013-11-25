package xapi.shell;

import java.util.concurrent.CountDownLatch;

import org.junit.BeforeClass;
import org.junit.Test;

import xapi.file.X_File;
import xapi.inject.impl.JavaInjector;
import xapi.io.api.LineReader;
import xapi.io.api.StringReader;
import xapi.log.X_Log;
import xapi.shell.api.ShellCommand;
import xapi.shell.api.ShellSession;
import xapi.test.Assert;
import xapi.time.X_Time;
import xapi.util.X_Namespace;
import xapi.util.api.SuccessHandler;

public class ShellTesting {

  @BeforeClass
  public static void prepare() {
    System.setProperty(X_Namespace.PROPERTY_MULTITHREADED, "10");
    JavaInjector.instance(ShellCommand.class);
  }

  @Test(timeout=10000)
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
    final LineReader e = new StringReader();
    final boolean[] success = new boolean[1];
    final ShellSession shell = X_Shell.newService().newCommand(
        "sh"
        ,"-ac",X_File.getResourceMaybeUnzip("xapi/sh.sh", null)
        )
        .run(new SuccessHandler<ShellSession>() {
          @Override
          public void onSuccess(ShellSession t) {
            success[0] = true;
          }
        }, null);
    
    shell.stdOut(d);
    shell.stdErr(e);
    shell.stdIn("echo \"test success\"");
    shell.stdIn("exit");
    d.waitToEnd();
    Assert.assertEquals("test success", d.toString().trim());
    Assert.assertTrue(success[0]);
    
//    X_Log.info(shell.stdIn("javac ~/XApi.java -d /tmp && cd /tmp; java XApi"));
  }

}
