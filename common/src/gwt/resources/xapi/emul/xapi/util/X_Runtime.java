package xapi.util;

import xapi.util.X_Namespace;

import com.google.gwt.core.client.GWT;

/**
 * This class is magic; there are four copies of this class.
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
public class X_Runtime {

  /**
   * Are we running in javascript?
   */
  public static boolean isJavaScript() {
    return GWT.isScript();
  }
  /**
   * Are we running in java
   */
  public static boolean isJava() {
    return !GWT.isScript();
  }
  /**
   * Flash will provide it's own super-sourced copy.
   */
  public static boolean isActionscript() {
    return false;
  }

  /**
   * Always true in super-source
   */
  public static boolean isGwt() {
    return true;
  }

  /**
   * Will return true if this method is swapped out with magic method injection.
   */
  public static boolean isRuntimeInjection() {
    return false;
  }

  public static boolean isDebug() {
    return "true".equals(System.getProperty(X_Namespace.PROPERTY_DEBUG, "false"));
  }

  public static boolean isMultithreaded() {
    return false;
  }

  public static String getWorkingDirectory() {
    return com.google.gwt.core.client.GWT.getModuleBaseURL();
  }

  public static String fileSeparator() {
    return "/";
  }

  public static char fileSeparatorChar() {
    return '/';
  }

}
