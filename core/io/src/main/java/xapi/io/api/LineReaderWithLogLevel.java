package xapi.io.api;

import xapi.log.api.HasLogLevel;
import xapi.log.api.LogLevel;

public abstract class LineReaderWithLogLevel extends SimpleLineReader implements HasLogLevel {

  private LogLevel logLevel = LogLevel.INFO;

  @Override
  public LogLevel getLogLevel() {
    return logLevel;
  }

  public LineReaderWithLogLevel setLogLevel(LogLevel logLevel) {
    this.logLevel = logLevel;
    return this;
  }
}
