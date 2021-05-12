package xapi.shell;

import xapi.inject.X_Inject;
import xapi.io.api.HasLiveness;
import xapi.io.api.LineReader;
import xapi.io.api.StringReader;
import xapi.log.X_Log;
import xapi.reflect.X_Reflect;
import xapi.shell.api.ShellSession;
import xapi.shell.service.ShellService;
import xapi.prop.X_Properties;
import xapi.string.X_String;

import java.io.File;

public class X_Shell {

  private static String[] args;
  private static Class<?> main;

  private X_Shell() {}

  public static ShellService newService() {
    return X_Inject.instance(ShellService.class);
  }

  public static HasLiveness liveChecker(final Process process) {
    return new HasLiveness() {

      @Override
      public boolean isAlive() {
        try {
          int exit = process.exitValue();
          X_Log.debug(X_Shell.class, "Process ended with exit code " + exit);
          return false;
        } catch (IllegalThreadStateException e) {
          return true;
        }
      }
    };
  }

  public static void rememberArgs(Class<?> mainClass, String... args) {
    X_Shell.args = args;
    main = mainClass;
  }

  /**
   * Launches a java process inside a shell environment.
   * Currently, only the unix shell works, using xapi-dev-shell/src/main/resources/xapi/sh.sh,
   * and is only tested on linux.
   *
   * @param mainClass - The class name with the main method to run
   * @param classpath - The classpath for this execution.
   * @return A {@link ShellSession} used to control the running process.
   */
  public static ShellSession launchJava(Class<?> mainClass, String[] classpath) {
    return launchJava(mainClass, classpath, new String[0], new String[0]);
  }

  /**
   * Launches a java process inside a shell environment.
   * Currently, only the unix shell works, using xapi-dev-shell/src/main/resources/xapi/sh.sh,
   * and is only tested on linux.
   *
   * @param mainClass - The class name with the main method to run
   * @param classpath - The classpath for this execution.
   * @param vmFlags   - Flags to pass to jvm (like system properties)
   * @param args      - Arguments to pass to main method
   * @return A {@link ShellSession} used to control the running process.
   */
  public static ShellSession launchJava(Class<?> mainClass, String[] classpath, String[] vmFlags, String[] args) {
    return launchJava(mainClass, classpath, vmFlags, args, new StringReader(), new StringReader());
  }
  /**
   * Launches a java process inside a shell environment.
   * Currently, only the unix shell works, using xapi-dev-shell/src/main/resources/xapi/sh.sh,
   * and is only tested on linux.
   *
   * @param mainClass - The class name with the main method to run
   * @param classpath - The classpath for this execution.
   * @param vmFlags   - Flags to pass to jvm (like system properties)
   * @param args      - Arguments to pass to main method
   * @param stdOut    - A line reader to accept output from process stdOut
   * @param stdErr    - A line reader to accept output from process stdErr
   * @return A {@link ShellSession} used to control the running process.
   */
  public static ShellSession launchJava(Class<?> mainClass, String[] classpath, String[] vmFlags, String[] args, LineReader stdOut, LineReader stdErr) {
    // Check if mainClass is in a folder or in a jar, so we know if we need to add it to the classpath
    try {
      exists_check:
      {
        String fileLoc = X_Reflect.getFileLoc(mainClass);

        for (String item : classpath) {
          if (item.equals(fileLoc)) {
            break exists_check;
          }
        }
        String[] newClasspath = new String[classpath.length + 1];
        System.arraycopy(classpath, 0, newClasspath, 0, classpath.length);
        newClasspath[classpath.length] = fileLoc;
        classpath = newClasspath;
      }
    } catch (Exception e) {
      X_Log.warn(ShellSession.class, "Error appending location of ", mainClass, "to classpath", classpath, e);
    }
    String javaHome = System.getProperty("java.home");
    String javaBin = javaHome +
        File.separator + "bin" +
        File.separator + "java";
    String[] javaArgs = new String[4 + (vmFlags == null ? 0 : vmFlags.length) + (args == null ? 0 : args.length)];
    javaArgs[0] = javaBin;
    int pos = 1;
    if (vmFlags != null && vmFlags.length > 0) {
      System.arraycopy(vmFlags, 0, javaArgs, pos, vmFlags.length);
      pos += vmFlags.length;
    }
    javaArgs[pos] = "-classpath";
    javaArgs[++pos] = X_String.join(File.pathSeparator, classpath).trim();
    javaArgs[++pos] = mainClass.getCanonicalName();
    X_Log.trace(X_Shell.class, "Running java command", mainClass, args);
    if (args != null && args.length > 0) {
      System.arraycopy(args, 0, javaArgs, ++pos, args.length);
    }
    X_Log.trace(X_Shell.class, "Java command", X_String.join(" ", javaArgs));
    return globalService().runInShell(
        false, stdOut, stdErr, javaArgs);
  }

  public static ShellService globalService() {
    return X_Inject.singleton(ShellService.class);
  }

  public static ShellSession launchInShell(String cmd, LineReader stdOut, LineReader stdErr) {
    X_Log.trace(X_Shell.class, "Running in shell\n", cmd);
    return globalService().runInShell(
        false
        , stdOut
        , stdErr, cmd);
  }

  public static void restartSelf() {
    if (main == null) {
      X_Log.error("Cannot use restartSelf until calling rememberArgs(Class<?> mainClass, String[] args)");
      return;
    }
    String[] arguments = (args == null) ? new String[0] : args;
    // Get our classpath.
    String cp = X_Properties.getProperty("java.class.path");
    if (cp == null) {
      X_Log.error("Unable to detect X_Property 'java.class.path'; unable to restart application");
      return;
    }
    // TODO find vm args somehow?  Take as parameter?  Just check a bunch of runtime settings?
    // Finding memory for Xmx would be simple enough, but we need to check management beans for better results
    launchJava(main, cp.split("[" + File.pathSeparator + "]"), new String[]{}, arguments);
  }

}
