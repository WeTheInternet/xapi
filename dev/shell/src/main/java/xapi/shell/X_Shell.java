package xapi.shell;

import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.jar.JarFile;

import xapi.inject.X_Inject;
import xapi.io.api.HasLiveness;
import xapi.io.api.StringReader;
import xapi.log.X_Log;
import xapi.shell.service.ShellService;
import xapi.source.X_Source;
import xapi.util.X_Debug;
import xapi.util.X_Properties;
import xapi.util.X_String;

public class X_Shell {

	private X_Shell(){}
	
	public static ShellService newService() {
		return X_Inject.instance(ShellService.class);
	}

	public static ShellService globalService() {
	  return X_Inject.singleton(ShellService.class);
	}

  public static HasLiveness liveChecker(final Process process) {
    return new HasLiveness() {
      
      @Override
      public boolean isAlive() {
        try{
          int exit = process.exitValue();
          X_Log.debug("Process ended with exit code "+exit);
          return false;
        } catch (IllegalThreadStateException e) {
          return true;
        }
      }
    };
  }
  
  private static String[] args;
  private static Class<?> main;
  public static void rememberArgs(Class<?> mainClass, String ... args) {
    X_Shell.args = args;
    main = mainClass;
  }
  /**
   * Attempts to launch a process external to our own.
   * 
   * Currently, only the unix shell works, using nohup, and is only tested on linux.
   * 
   * @param mainClass - The class name with the main method to run
   * @param classpath - The classpath for this execution.
   * @param vmFlags - Flags to pass to jvm (like system properties)
   * @param args - Arguments to pass to main method
   * 
   * TODO: pipe external process to a known file, so we can stream back logs.
   */
  public static void launchExternal(Class<?> mainClass, String[] classpath, String[] vmFlags, String[] args) {
    // Check if mainClass is in a folder or in a jar, so we know if we need to use -jar on it.
    URL loc = mainClass.getProtectionDomain().getCodeSource().getLocation();
    boolean isJar = loc.getProtocol().equals("jar") || loc.toExternalForm().contains("jar");
    
    if (isJar) {
      
    } else {
      String cmd = "java -cp "+X_String.join(File.pathSeparator, classpath)
          +" "+X_String.join(" ", vmFlags)+" "+ mainClass.getCanonicalName()
          +" "+X_String.join(" ", args);
      X_Log.info(cmd);
      // just run as a file
//      ShellResult result = 
      globalService().runInShell(
          cmd
          ,new StringReader(), new StringReader());
    }
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
    // TODO find vm args somehow?  Take as parameter?
    launchExternal(main, cp.split("["+File.pathSeparator+"]"), new String[]{}, arguments);
  }

  public static String getResourceMaybeUnzip(String resource,
      ClassLoader cl) {
    if (cl == null)
      cl = Thread.currentThread().getContextClassLoader();
    URL url = cl.getResource(resource);
    try {
      if (url == null)
        throw new RuntimeException("Resource "+resource +" not available on classpath.");
      if (url.getProtocol().equals("file")) {
        String loc = url.toExternalForm();
        if (loc.contains("jar!")) {
          return unzip(resource, new JarFile(X_Source.stripJarName(loc)));
        } else {
          return X_Source.stripFileName(loc);
        }
      } else if (url.getProtocol().equals("jar")) {
        return unzip(resource, ((JarURLConnection)(url.openConnection())).getJarFile());
      } else {
        X_Log.warn("Unknown get resource protocol "+url.getProtocol());
      }
    } catch (Throwable e) {
      X_Log.error("Error trying to load / unzip resouce "+resource+" using file "+url, e);
      X_Debug.maybeRethrow(e);
    }
    return null;
  }

  private static String unzip(String resource, JarFile jarFile) {
    return null;
  }
  
	
}
