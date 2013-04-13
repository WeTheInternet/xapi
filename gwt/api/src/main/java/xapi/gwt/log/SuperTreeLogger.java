package xapi.gwt.log;

import xapi.log.X_Log;
import xapi.log.api.LogLevel;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.TreeLogger.HelpInfo;
import com.google.gwt.core.ext.TreeLogger.Type;

public class SuperTreeLogger {

  public static void log(TreeLogger treeLogger, Type type, String msg, Throwable caught,
      HelpInfo helpInfo) {
    switch(type) {
    case ALL:
    case DEBUG:
    case SPAM:
      X_Log.debug(msg, caught);
      break;
    case TRACE:
      X_Log.trace(msg, caught);
      break;
    case INFO:
      X_Log.info(msg, caught);
      break;
    case WARN:
      X_Log.warn(msg, caught);
      break;
    case ERROR:
      X_Log.error(msg, caught);
      break;
    }
  }

  public static boolean isLoggable(TreeLogger treeLogger, Type type) {
    LogLevel check = X_Log.logLevel();
    if (check == null)check = LogLevel.INFO;
    return type.ordinal()>check.ordinal();
  }

  public static TreeLogger branch(TreeLogger treeLogger, Type type, String msg, Throwable caught, HelpInfo helpInfo) {
    return treeLogger;
  }

}
