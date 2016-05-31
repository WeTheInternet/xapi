package xapi.gwt.util;

import xapi.log.X_Log;

public class X_Gwt {

  private X_Gwt(){}

  /**
   * Runs a main method;
   * dev mode will simply use reflection to call .main(args);
   * prod mode can magic-method-inject the main method into a web worker / iframe.
   *
   * @param cls - The class with the main method to run.  Use a class literal.
   * @param args - The strings you want passed to the main()
   */
  public static void runMainMethod(Class<?> cls, String ... args) {
    // This is for dev mode only, or prod mode if you don't use a class literal.
    try{
      cls.getMethod("main", String[].class).invoke(null, new Object[]{args});
    } catch (Exception e) {
      X_Log.error("Error running main method in",cls,"with args",args);
    }
  }

}
