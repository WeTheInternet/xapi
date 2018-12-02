package xapi.fu.log;

import xapi.fu.inject.Injectable;
import xapi.fu.log.Log.LogLevel;

import static xapi.fu.log.Log.printLevel;

/**
 * The class responsible for supplying global Log instances.
 *
 * To chose your own subclass, the first classpath entry of
 * META-INF/singletons/xapi.fu.log.LogInjector will win.
 *
 * This is different than "standard X_Inject",
 * where we load all available types, and pick the highest priority
 *
 * Create this file containing the classname of the LogInjector you want us to `new`
 * (or, a xapi.fu.inject.Injectable which itself creates a LogInjector).
 * Note that if you implement Injectable, you will get a ClassLoader as your first argument.
 *
 * If you are a poor soul who lives in ClassLoaderHell(tm),
 * you will want to implement Injectable and create your instance from the supplied ClassLoader.
 *
 *
 * Note that the only thing you should do with this class
 * is extend and override it; you should not access it directly.
 *
 * Instead use {@link Log#firstLog(Object...)} to prefer searching
 * for a method-local Log instance, which may have different levels or renderers.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 9/20/18 @ 10:04 PM.
 */
public class LogInjector {

  // Some default implementations for when you are lazy.
  // We stash them in this ugly package-private class,
  // to encourage the use of static methods in the Log class,
  // Log.defaultLogger() or Log.sysOut/Err.
  // This will allow magic-method injection to swap these out, if desired

  static volatile LogInjector instance;

  static LogInjector inst() {
    if (instance == null) {
      // only pay to sync memory the first time
      synchronized (LogInjector.class) {
        if (instance == null) {
          instance = createInstance();
        }
      }
    }
    // pay a volatile read to avoid resyncing whole thread
    return instance;
  }

  private static LogInjector createInstance() {
    return Injectable.injectStatic(LogInjector.class, LogInjector::new);
  }

  static final Log VOID = (level, debug) -> {};

  // Hm... should we actually capture System.out immediately, in case it changes?
  static final Log SYS_OUT = (level, debug) ->
      System.out.println(printLevel(level) + debug);

  static final Log SYS_ERR = (level, debug) ->
      System.err.println(printLevel(level) + debug);

  static final Log DEFAULT = inst().defaultLogger();

  public Log defaultLogger() {
    return (level, debug) ->
        (level == LogLevel.ERROR ? SYS_ERR : SYS_OUT)
            .print(level, debug);
  }

}
