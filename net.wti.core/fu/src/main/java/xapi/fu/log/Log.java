package xapi.fu.log;

import xapi.fu.Debuggable;
import xapi.fu.api.Ignore;

import static xapi.fu.log.Log.printLevel;

/**
 * A logging functional interface, complete with glue for static injection of a logger,
 * and utility methods for finding a logger from arbitrary method-local objects {@link #firstLog(Object...)}.
 *
 * The functional interface method just gets a log level and a string; any kind of templating and brains
 * can be configured by overriding the default methods.
 *
 * To control the default logger, see {@link LogInjector}
 *
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
@FunctionalInterface
public interface Log extends Debuggable {

  enum LogLevel {
    ALL, DEBUG, TRACE, INFO, WARN, ERROR
    ;

    public boolean isLoggable(LogLevel level) {
      return level.ordinal() >= ordinal();
    }

    static LogLevel DEFAULT = valueOf(System.getProperty("xapi.log.level", "INFO"));

  }

  interface DefaultLog extends Log {
    @Override
    default void print(LogLevel level, String debug) {
      LogInjector.DEFAULT.log(level, debug);
    }
  }

  default Log log(LogLevel level, Object ... values) {
    if (isLoggable(level)) {
      print(level, debug(values));
    }
    return this;
  }

  default Log log(Class forClass, LogLevel level, Object ... values) {
    if (isLoggable(level)) {
      log(forClass, debug(values));
    }
    return this;
  }

  default Log log(Class forClass, Object ... values) {
    LogLevel level = levelForClass(forClass);
    if (isLoggable(level)) {
      print(level, debug(values));
    }
    return this;
  }

  default LogLevel levelForClass(Class forClass) {
    gen_logLevelForClass : {
      // This level will always be loggable,
      // but note the named block here;
      // use of this label allows us to mark anywhere we want to generate code
      // that translates a class parameter into a log level.
      // This functionality is being developed in a separate repository,
      // but this code is left here so this class can be used in testing that javac source transform plugin
      return getLogLevel();
    }
  }

  // This tells our ModelMagic to ignore this method if it encountered in a Model class.
  @Ignore("model") // we can't see IsModel.NAMESPACE from this module, so use "magic string constant"
  void print(LogLevel level, String msg);

  default String maybePrintLevel(LogLevel level) {
    return printLevel(level);
  }

  default boolean isLoggable(LogLevel level) {
    LogLevel myLevel = getLogLevel();
    return myLevel.ordinal() <= level.ordinal();
  }

  default LogLevel getLogLevel() {
    return LogLevel.DEFAULT;
  }

  static String printLevel(LogLevel level) {
    return "["+level+"] ";
  }

  static Log normalize(Log log) {
    return log == null ? defaultLogger() : log;
  }

  static Log allLogs(Object ... maybeLogs) {
    Log stacked = null;
    for (Object maybeLog : maybeLogs) {
      if (maybeLog instanceof Log) {
        final Log log = (Log) maybeLog;
        if (stacked == null) {
          stacked = log;
        } else {
          final Log parents = stacked;
          stacked = (level, debug) -> {
            parents.log(level, debug);
            log.log(level, debug);
          };
        }
      }
    }
    return stacked == null ? defaultLogger() : stacked;
  }

  static Log firstLog(Object ... log) {
    if (log == null) {
      // someone might send us a null array reference...
      return defaultLogger();
    }
    for (Object maybe : log) {
      if (maybe == null) {
        continue;
      }
      if (maybe.getClass().isArray()) {
        final Class<?> cmp = maybe.getClass().getComponentType();
        if (cmp != null && cmp.isPrimitive()) {
          // no logs to see here...  null check is for gwt / optimized / stripped builds.
          continue;
        }
        maybe = firstLog((Object[])maybe);
        if (maybe == defaultLogger()) {
          continue;
        }
      }
      if (maybe instanceof Log) {
        return (Log) maybe;
      }
    }
    return defaultLogger();
  }

  static Log defaultLogger() {
    return LogInjector.DEFAULT;
  }

  static Log sysOut() {
    return LogInjector.SYS_OUT;
  }

  static Log sysErr() {
    return LogInjector.SYS_ERR;
  }

  static Log voidLogger() {
    return LogInjector.VOID;
  }

  static void tryLog(Class<?> cls, Object inst, Object ... e) {
    loggerFor(cls, inst).log(cls, e);
  }

  static Log loggerFor(Class<?> cls, Object inst) {
    if (inst instanceof Log) {
      return (Log)inst;
    }
    // TODO add a @Level(INFO) annotation to check for;
    // we should check first on the class of the instance,
    // then on the calling class,
    // then on any other supertypes, packages or super-packages.
    return LogInjector.DEFAULT;
  }

  static void tryLog(Class<?> cls, Object inst, LogLevel level, Object ... e) {
    loggerFor(cls, inst).log(cls, level, e);
  }

  @Override
  default String coerceNonArray(Object obj, boolean first) {
    if (first && obj instanceof Class) {
      // classes get special treatment...
      return Debuggable.classLink((Class<?>) obj);
    }
    return Debuggable.super.coerceNonArray(obj, first);
  }
}

