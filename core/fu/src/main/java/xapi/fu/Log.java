package xapi.fu;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 07/11/15.
 */
public interface Log extends Debuggable {
  enum LogLevel {
    DEBUG, TRACE, INFO, WARN, ERROR
    ;

    static LogLevel DEFAULT = valueOf(System.getProperty("xapi.log.level", "WARN"));

  }
  default Log log(LogLevel level, Object ... values) {
    if (isLoggable(level)) {
      print(level, debug(values));
    }
    return this;
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
