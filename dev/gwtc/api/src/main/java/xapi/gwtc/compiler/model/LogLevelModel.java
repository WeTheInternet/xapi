package xapi.gwtc.compiler.model;

import xapi.log.api.LogLevel;
import xapi.model.api.Model;

public interface LogLevelModel extends Model{

  LogLevel logLevel();
  void setLogLevel(LogLevel level);

}
