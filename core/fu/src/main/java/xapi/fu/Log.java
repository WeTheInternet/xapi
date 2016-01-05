package xapi.fu;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public interface Log extends Debuggable {
  enum LogLevel {
    DEBUG, TRACE, INFO, WARN, ERROR
    ;

    static LogLevel DEFAULT = valueOf(System.getProperty("xapi.log.level", "INFO"));

  }
  default Log log(LogLevel level, Object ... values) {
    if (isLoggable(level)) {
      print(level, debug(values));
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

  default void print(LogLevel level, String debug) {
    String maybe = maybePrintLevel(level);
    if (level == LogLevel.ERROR) {
      System.err.println(maybe+debug);
    } else {
      System.out.println(maybe+debug);
    }
  }

  default String maybePrintLevel(LogLevel level) {
    return "["+level+"] ";
  }

  default boolean isLoggable(LogLevel level) {
    LogLevel myLevel = getLogLevel();
    return myLevel.ordinal() <= level.ordinal();
  }

  default LogLevel getLogLevel() {
    return LogLevel.DEFAULT;
  }

}
