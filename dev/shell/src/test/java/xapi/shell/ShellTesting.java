package xapi.shell;

import java.util.concurrent.CountDownLatch;

import javax.swing.SwingUtilities;

import org.junit.BeforeClass;
import org.junit.Test;

import xapi.inject.impl.JavaInjector;
import xapi.io.api.LineReader;
import xapi.io.api.StringReader;
import xapi.log.X_Log;
import xapi.shell.api.ShellCommand;
import xapi.shell.api.ShellResult;
import xapi.time.X_Time;
import xapi.util.X_Namespace;
import xapi.util.api.SuccessHandler;

public class ShellTesting {

  @BeforeClass
  public static void prepare() {
    System.setProperty(X_Namespace.PROPERTY_MULTITHREADED, "10");
    JavaInjector.instance(ShellCommand.class);
  }

  @Test
  public void testShell() throws Exception {
    
    final CountDownLatch job = new CountDownLatch(1);
    SwingUtilities.invokeAndWait(new Runnable() {
      @Override
      public void run() {
        try{
          runTest();
        } catch (Exception e){
          e.printStackTrace();
        }
        job.countDown();
      }
    });
    job.await();
  }
  private void runTest() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    final ShellResult shell = X_Shell.newService().newCommand(
        "sh"
        ,"-ac",X_Shell.getResourceMaybeUnzip("xapi/sh.sh", null)
        )
        .run(new SuccessHandler<ShellResult>() {
          @Override
          public void onSuccess(ShellResult t) {
            latch.countDown();
            X_Log.info(t);
          }
        }, null);
    
    X_Time.runLater(new Runnable() {
      @Override
      public void run() {
        LineReader d = new StringReader();
        shell.stdOut(d);
        LineReader e = new StringReader();
        shell.stdErr(e);
//        X_Log.info(shell.stdIn("echo \"test success\""));
      }
    });
    X_Log.info(shell.stdIn("echo \"test success\""));
    latch.await();
//    X_Time.trySleep(5000, 0);
//    X_Log.info(shell.stdIn("echo \"test success\""));
//    X_Log.info(shell.stdIn("javac ~/XApi.java -d /tmp && cd /tmp; java XApi"));
  }

}
