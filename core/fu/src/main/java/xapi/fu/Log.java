package xapi.fu;

import xapi.fu.Log.LogLevel;

import static xapi.fu.Log.printLevel;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
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
      DefaultLoggers.DEFAULT.log(level, debug);
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
      log(forClass, values);
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
      // use of this label allows use to mark anywhere we want to generate code
      // that translates a class parameter into a log level.
      // This functionality is being developed in a separate repository,
      // but this code is left here so this class can be used in testing that javac source transform plugin
      return getLogLevel();
    }
  }

  void print(LogLevel level, String debug);

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
      if (maybe instanceof Log) {
        return (Log) maybe;
      }
    }
    return defaultLogger();
  }

  static Log defaultLogger() {
    return DefaultLoggers.DEFAULT;
  }

  static Log sysOut() {
    return DefaultLoggers.SYS_OUT;
  }

  static Log sysErr() {
    return DefaultLoggers.SYS_ERR;
  }

  static Log voidLogger() {
    return DefaultLoggers.VOID;
  }
}

interface DefaultLoggers {

  // Some default implementations for when you are lazy.
  // We stash them in this ugly package-private class,
  // to encourage the use of static methods in the Log class,
  // Log.defaultLogger() or Log.sysOut/Err.
  // This will allow magic-method injection to swap these out, if desired

  Log VOID = (level, debug) -> {};

  Log SYS_OUT = (level, debug) ->
      System.out.println(printLevel(level) + debug);

  Log SYS_ERR = (level, debug) ->
      System.err.println(printLevel(level) + debug);

  Log DEFAULT = (level, debug) ->
      (level == LogLevel.ERROR ? SYS_OUT : SYS_ERR)
          .print(level, debug);

}
