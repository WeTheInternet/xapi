package xapi.adapter.log.gwt;

import xapi.collect.fifo.Fifo;
import xapi.log.api.LogLevel;
import xapi.log.api.LogService;
import xapi.log.impl.JreLog;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.util.log.CompositeTreeLogger;

public class GwtLogAdapter extends CompositeTreeLogger {

  private final LogService service;
  private boolean logToChildren;

  public GwtLogAdapter(LogService service, TreeLogger ... children) {
    super(children);
    this.service = service == null ? new JreLog() : service;
  }


  @Override
  public boolean isLoggable(Type type) {
    switch (type) {
      case ALL:
      case TRACE:
        return service.shouldLog(LogLevel.ALL);
      case DEBUG:
        return service.shouldLog(LogLevel.DEBUG);
      case ERROR:
        return service.shouldLog(LogLevel.ERROR);
      case INFO:
        return service.shouldLog(LogLevel.INFO);
      case SPAM:
        return service.shouldLog(LogLevel.TRACE);
      case WARN:
        return service.shouldLog(LogLevel.WARN);
    }
    return isLogToChildren() && super.isLoggable(type);
  }

  @Override
  public void log(Type type, String msg, Throwable caught, HelpInfo helpInfo) {
    Fifo<Object> fifo = service.newFifo();
    LogLevel level = toLevel(type);
    fifo.give(msg);
    if (caught != null)
      fifo.give(service.unwrap(level, caught));
    if (helpInfo != null) {
      fifo.give(helpInfo.getPrefix());
      if (helpInfo.getURL() != null) {
        fifo.give("\n");
        if (helpInfo.getAnchorText() != null) {
          fifo.give(helpInfo.getAnchorText());
        }
        fifo.give("Help URL").give(helpInfo.getURL().toExternalForm());
      }
    }
    service.doLog(level, fifo);
    if (isLogToChildren())
      super.log(type, msg, caught, helpInfo);
  }


  public static LogLevel toLevel(Type type) {
    switch (type) {
      case ALL:
      case SPAM:
        return LogLevel.ALL;
      case TRACE:
        return LogLevel.TRACE;
      case DEBUG:
        return LogLevel.DEBUG;
      case ERROR:
        return LogLevel.ERROR;
      case INFO:
        return LogLevel.INFO;
      case WARN:
        return LogLevel.WARN;
    }
    throw new UnsupportedOperationException();
  }


  public boolean isLogToChildren() {
    return logToChildren;
  }


  public void setLogToChildren(boolean logToChildren) {
    this.logToChildren = logToChildren;
  }

}
