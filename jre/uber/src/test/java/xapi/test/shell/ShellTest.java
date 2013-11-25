package xapi.test.shell;

import java.io.File;
import java.util.Arrays;

import org.junit.Test;

import xapi.io.api.StringReader;
import xapi.log.X_Log;
import xapi.shell.X_Shell;
import xapi.shell.api.ShellSession;
import xapi.test.Assert;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.util.X_Namespace;

public class ShellTest {

  static {
    System.setProperty(X_Namespace.PROPERTY_MULTITHREADED, "10");
  }
  private static final String success = "success";
  private static final int exitStatus = 111;
  
  public static void main(String[] args) {
    System.out.print(success);
    System.exit(exitStatus);// Test custom exit status
  }
  
  @Test(timeout=5000)
  public void testShell() {
    Moment start = X_Time.now();
    String[] classpath = System.getProperty("java.class.path").split(File.pathSeparator);
    StringReader reader = new StringReader();
    ShellSession result = X_Shell.launchJava(ShellTest.class, classpath, new String[]{"-Xmx128M"}, new String[0]);
    result.stdOut(reader);
    X_Log.debug(getClass(), "Bootstrap shell", X_Time.difference(start));
    try {
      reader.waitToEnd(4000, 0);
    } catch (InterruptedException e) {
      Assert.fail("Interrupted while waiting for process to end");
    }
    Assert.assertEquals(success, reader.toString());
    Assert.assertEquals(exitStatus, result.join());
    X_Log.trace(getClass(), "Process Runtime", X_Time.difference(start));
  }
  
}
