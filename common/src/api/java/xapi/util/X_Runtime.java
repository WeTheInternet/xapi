package xapi.util;

import xapi.constants.X_Namespace;
import xapi.platform.JrePlatform;
import xapi.string.X_String;

import static xapi.constants.X_Namespace.PROPERTY_DEBUG;
import static xapi.constants.X_Namespace.PROPERTY_MULTITHREADED;
import static xapi.constants.X_Namespace.PROPERTY_TEST;
import static xapi.constants.X_Namespace.PROPERTY_USE_X_INJECT;

import java.io.File;
import java.io.IOException;

/**
 * This class is magic; there are four copies of this class;
 * do not add anything to them unless you download all xapi source,
 * and do a full text search on "X_Runtime\s+{"
 *
 * This is the public class exposed to a jre.
 * Each method compiles down to a runtime constant,
 * or as close a possible,
 * to encourage compiler inlining.
 *
 * Then, we super-source a copy for normal gwt builds, which reports false
 * for isRuntimeInjection, and GWT.isScript() for isGwtProd().
 *
 * For gwt users who inherit xapi.X_Inject, we use magic method injection
 * to replace the super-sourced copy of {@link #isRuntimeInjection()} to return
 * true or false, depending on the configuration present in gwt module xml.
 *
 * Finally, our xapi-debug module will override X_Runtime in the classloader,
 * to return true for isDebug().
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@JrePlatform
public class X_Runtime {

  private X_Runtime() {}

  // using static final fields to encourage inlining after clinit
  private static final boolean inject;
  private static final boolean parallel;
  private static final boolean debug;
  private static final boolean test;
  private static final boolean gwt;

  static {
    debug = "true".equals(System.getProperty(PROPERTY_DEBUG, "false"));
    test = "true".equals(System.getProperty(PROPERTY_TEST, "false"));

    boolean success = "true".equals(System.getProperty(PROPERTY_USE_X_INJECT, "true"));
    try {

      // Check if user has disabled with a system property.
      if (success) {
        // Make sure the X_Inject class is loadable
        Class.forName("xapi.inject.X_Inject");
      }
    } catch (Throwable e) {
      String error = "Class xapi.inject.X_Inject is not loadable." +
        (debug?"\nEnsure your module inherits xapi-inject, or set system property " +
                 PROPERTY_USE_X_INJECT+" to \"false\"." : "");
      if (debug || success)
        System.err.println(error);
      // If the user explicitly set use X inject to true, we bail early,
      // with a more useful error message.  Not setting the property will
      // merely print the error message, and continue w/out injection.
      if ("true".equals(System.getProperty(PROPERTY_USE_X_INJECT)))
        throw new AssertionError(error);
      success = false;
    }
    inject = success;

    try {
      // Make sure X_Process is loadable, and bail with a usable error message.
      Class.forName("xapi.process.X_Process");
      success = !"".equals(System.getProperty(PROPERTY_MULTITHREADED,"1"));
    } catch (Throwable e) {
      success = !"".equals(System.getProperty(PROPERTY_MULTITHREADED,""));
      String message = "Class xapi.process.X_Process is not loadable." +
          (debug?"\nEnsure your module inherits xapi-core-process, or set system property " +
              PROPERTY_MULTITHREADED+" to empty string \"\".":"");
      if (debug || success)
        System.err.println(message);
      if (success) { //if system property was set
        // throw now, with better error message.
        throw new AssertionError(message);
      }
      success = false;
    }
    parallel = success;

    // our last variable, isGwt, requires special handling;
    // although this class should be super-sourced in all gwt compiles,
    // we must take care not to break on any platforms that don't have gwt-user
    // on the classpath (android comes to mind).
    success = false;
    try {
      //This will bomb in gwt if our emulation layer isn't on user classpath
      Class<?> cls = Class.forName("com.google.gwt.core.shared.GWT");
      if (cls == null) {
        //Only gwt might return null
        //in cases where a user has our emulation layer on classpath,
        //but didn't explicitly inherit wetheinter.net.gwt.Reflect;
        //Class.forName will return null instead of throw ClassNotFoundException
        success = true;
        //For users who do inherit wetheinter.net.gwt.Reflect,
        //Class.forName is a magic-method, which will return any class
        //accessible to the module being compiled
      } else {

      try {
        java.lang.reflect.Method isClient = cls.getMethod("isClient");
        //normally only gwt dev or a jre that happens to have GWT on classpath
        //will actually get to call this method.  Gwt production might,
        //if you send the GWT.class literal through X_Gwt.magicClass() method.
        success = (Boolean)isClient.invoke(null);
      }catch (NoSuchMethodError e) {
        //This is what our emulated class will throw if a class is not enhanced
        success = true;
      } catch (IllegalArgumentException e) {
        assert false : e;//should never get here
      } catch (IllegalAccessException e) {
        //eat this silently; a java enviro with a security manager is not gwt
      } catch (java.lang.reflect.InvocationTargetException e) {
        assert false : e;//should never get here, but don't punish prod
      }
      }
    } catch (VerifyError e) {
      // only ever thrown by java; can happen when gwt w/ java 8 is on classpath
    } catch (UnsupportedClassVersionError e) {
      // only ever thrown by java; can happen when gwt w/ java 8 is on classpath
    }catch(NoSuchMethodException e) {
      //What an unpatched GWT java.lang.Class will throw
      success = true;
    }catch(NoClassDefFoundError e) {
      //A jre that does not have GWT on classpath
    }catch(ClassNotFoundException e) {
      //A jre that does not have GWT on classpath
    }
    gwt = success;

  }
  /**
   * Overridden in super-source to return GWT.isScript()
   * @return - true if running in compiled javascript.
   */
  public static boolean isJavaScript() {
    return false;
  }
  /**
   * Overridden in super-source to return false.
   * @return - true in all jre runtimes, including gwt-dev
   */
  public static boolean isJava() {
    return true;
  }
  /**
   * @return - true only if xapi-flash-api is inherited.
   */
  public static boolean isActionScript() {
    return false;
  }

  /**
   * @return Convenience method for whether this is a gwt environment;
   * hardcoded to true in super-source; optimized to return true in gwt dev.
   */
  public static boolean isGwt() {
    return gwt;
  }

  /**
   * In jres, this returns true if xapi.inject.X_Inject is on classpath,
   * and the user has not set system property
   * {@link X_Namespace#PROPERTY_USE_X_INJECT} explicitly to "false".
   *
   * Overridden in super-source to return false;
   * overridden again with magic-method-injection to return true.
   *
   * The default debug module will check X_Properties on every call.
   *
   * @return true if runtime injection is enabled.
   */
  public static boolean isRuntimeInjection() {
    // defaults to true; can be inlined after clinit
    return inject;
  }


  /**
   * @return true is xapi.debug property is set to true.
   */
  public static boolean isDebug() {
    //set xapi.debug = true to enable debugging.
    return debug;
  }

  public static boolean isTest() {
    return test;
  }

  /**
   * @return true unless {@link X_Namespace#PROPERTY_MULTITHREADED} is set to 0
   */
  public static boolean isMultithreaded() {
    // default to true; can be inlined after clinit
    return parallel;
  }

  public static String getWorkingDirectory() {
    String pwd = System.getenv("PWD");
    if (pwd == null)
      try {
        pwd = new File(".").getCanonicalPath();
      } catch (IOException ignored) {}
    return pwd == null ? "." : pwd;
  }

  public static String fileSeparator() {
    return X_String.FILE_SEPARATOR;
  }

  public static char fileSeparatorChar() {
    return X_String.FILE_SEPARATOR.charAt(0);
  }
}
