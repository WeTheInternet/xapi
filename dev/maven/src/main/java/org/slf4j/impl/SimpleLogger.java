package org.slf4j.impl;

import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

import xapi.log.X_Log;
import xapi.log.api.LogLevel;


public class SimpleLogger extends MarkerIgnoringBase {
  /**
   * Mark the time when this class gets loaded into memory.
   */
  private static long startTime = System.currentTimeMillis();
  public static final String LINE_SEPARATOR =
    System.getProperty("line.separator");
  String name;

  /**
   * Package access allows only {@link SimpleLoggerFactory} to instantiate
   * SimpleLogger instances.
   */
  SimpleLogger(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  /**
   * This is our internal implementation for logging regular (non-parameterized)
   * log messages.
   *
   * @param level
   * @param message
   * @param t
   */
  private void log(LogLevel level, String message, Throwable t) {
    if (!X_Log.loggable(level))
      return;
    StringBuffer buf = new StringBuffer();

    long millis = System.currentTimeMillis();
    buf.append(millis - startTime);
    
    buf.append(" [");
    buf.append(level);
    buf.append("] ");

    buf.append(" {");
    buf.append(Thread.currentThread().getName());
    buf.append("} ");

    buf.append(level);
    buf.append(" ");

    buf.append(name);
    buf.append(" - ");

    buf.append(message);

    buf.append(LINE_SEPARATOR);

    System.err.print(buf.toString());
    if (t != null) {
      t.printStackTrace(System.err);
    }
    System.err.flush();
  }

  /**
   * For formatted messages, first substitute arguments and then log.
   *
   * @param level
   * @param format
   * @param param1
   * @param param2
   */
  private void formatAndLog(
    LogLevel level, String format, Object arg1, Object arg2) {
    String message = MessageFormatter.format(format, arg1, arg2);
    log(level, message, null);
  }

  /**
   * For formatted messages, first substitute arguments and then log.
   *
   * @param level
   * @param format
   * @param argArray
   */
  private void formatAndLog(LogLevel level, String format, Object[] argArray) {
    String message = MessageFormatter.arrayFormat(format, argArray);
    log(level, message, null);
  }

  /**
   * Always returns false.
   * @return always false
   */
  public boolean isDebugEnabled() {
    return X_Log.loggable(LogLevel.DEBUG);
  }

  /**
   * A simple implementation which logs messages of level DEBUG according
   * to the format outlined above.
   */
  public void debug(String msg) {
    log(LogLevel.INFO, msg, null);
  }

  /**
   * Perform single parameter substitution before logging the message of level
   * DEBUG according to the format outlined above.
   */
  public void debug(String format, Object arg) {
    formatAndLog(LogLevel.DEBUG, format, arg, null);
  }

  /**
   * Perform double parameter substitution before logging the message of level
   * DEBUG according to the format outlined above.
   */
  public void debug(String format, Object arg1, Object arg2) {
    formatAndLog(LogLevel.DEBUG, format, arg1, arg2);
  }

  /**
   * Perform double parameter substitution before logging the message of level
   * DEBUG according to the format outlined above.
   */
  public void debug(String format, Object[] argArray) {
    formatAndLog(LogLevel.DEBUG, format, argArray);
  }


  /**
   * Log a message of level DEGUB, including an exception.
   */
  public void debug(String msg, Throwable t) {
    log(LogLevel.DEBUG, msg, t);
  }

  /**
   * Return true based on X_Log global setting
   */
  public boolean isInfoEnabled() {
    return X_Log.loggable(LogLevel.INFO);
  }

  /**
   * A simple implementation which logs messages of level INFO according
   * to the format outlined above.
   */
  public void info(String msg) {
    log(LogLevel.INFO, msg, null);
  }

  /**
   * Perform single parameter substitution before logging the message of level
   * INFO according to the format outlined above.
   */
  public void info(String format, Object arg) {
    formatAndLog(LogLevel.INFO, format, arg, null);
  }

  /**
   * Perform double parameter substitution before logging the message of level
   * INFO according to the format outlined above.
   */
  public void info(String format, Object arg1, Object arg2) {
    formatAndLog(LogLevel.INFO, format, arg1, arg2);
  }

  /**
   * Perform double parameter substitution before logging the message of level
   * INFO according to the format outlined above.
   */
  public void info(String format, Object[] argArray) {
    formatAndLog(LogLevel.INFO, format, argArray);
  }


  /**
   * Log a message of level INFO, including an exception.
   */
  public void info(String msg, Throwable t) {
    log(LogLevel.INFO, msg, t);
  }


  /**
   * Always returns false.
   * @return always false
   */
  public boolean isTraceEnabled() {
    return X_Log.loggable(LogLevel.TRACE);
  }

  /**
   * A simple implementation which logs messages of level TRACE according
   * to the format outlined above.
   */
  public void trace(String msg) {
    log(LogLevel.TRACE, msg, null);
  }

  /**
   * Perform single parameter substitution before logging the message of level
   * TRACE according to the format outlined above.
   */
  public void trace(String format, Object arg) {
    formatAndLog(LogLevel.TRACE, format, arg, null);
  }

  /**
   * Perform double parameter substitution before logging the message of level
   * TRACE according to the format outlined above.
   */
  public void trace(String format, Object arg1, Object arg2) {
    formatAndLog(LogLevel.TRACE, format, arg1, arg2);
  }

  /**
   * Perform double parameter substitution before logging the message of level
   * TRACE according to the format outlined above.
   */
  public void trace(String format, Object[] argArray) {
    formatAndLog(LogLevel.TRACE, format, argArray);
  }


  /**
   * Log a message of level TRACE, including an exception.
   */
  public void trace(String msg, Throwable t) {
    log(LogLevel.TRACE, msg, t);
  }

  /**
   * Return true based on X_Log global setting
   */
  public boolean isWarnEnabled() {
    return X_Log.loggable(LogLevel.WARN);
  }

  /**
   * A simple implementation which logs messages of level WARN according
   * to the format outlined above.
  */
  public void warn(String msg) {
    log(LogLevel.WARN, msg, null);
  }

  /**
   * Perform single parameter substitution before logging the message of level
   * WARN according to the format outlined above.
   */
  public void warn(String format, Object arg) {
    formatAndLog(LogLevel.WARN, format, arg, null);
  }

  /**
   * Perform double parameter substitution before logging the message of level
   * WARN according to the format outlined above.
   */
  public void warn(String format, Object arg1, Object arg2) {
    formatAndLog(LogLevel.WARN, format, arg1, arg2);
  }

  /**
   * Perform double parameter substitution before logging the message of level
   * WARN according to the format outlined above.
   */
  public void warn(String format, Object[] argArray) {
    formatAndLog(LogLevel.WARN, format, argArray);
  }

  /**
   * Log a message of level WARN, including an exception.
   */
  public void warn(String msg, Throwable t) {
    log(LogLevel.WARN, msg, t);
  }

  /**
   * Return true based on X_Log global setting
   */
  public boolean isErrorEnabled() {
    return X_Log.loggable(LogLevel.ERROR);
  }

  /**
   * A simple implementation which logs messages of level ERROR according
   * to the format outlined above.
   */
  public void error(String msg) {
    log(LogLevel.ERROR, msg, null);
  }

  /**
   * Perform single parameter substitution before logging the message of level
   * ERROR according to the format outlined above.
   */
  public void error(String format, Object arg) {
    formatAndLog(LogLevel.ERROR, format, arg, null);
  }

  /**
   * Perform double parameter substitution before logging the message of level
   * ERROR according to the format outlined above.
   */
  public void error(String format, Object arg1, Object arg2) {
    formatAndLog(LogLevel.ERROR, format, arg1, arg2);
  }

  /**
   * Perform double parameter substitution before logging the message of level
   * ERROR according to the format outlined above.
   */
  public void error(String format, Object[] argArray) {
    formatAndLog(LogLevel.ERROR, format, argArray);
  }


  /**
   * Log a message of level ERROR, including an exception.
   */
  public void error(String msg, Throwable t) {
    log(LogLevel.ERROR, msg, t);
  }
}
