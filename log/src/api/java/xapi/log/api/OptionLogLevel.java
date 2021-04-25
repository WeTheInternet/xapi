package xapi.log.api;

/**
 * Option to set the tree logger log level.
 * 
 * Based on original implementation from the GWT project.
 *  
 * @author GWT team "gwtproject.org"
 * @author James X. Nelson "james@wetheinter.net"
 *
 */
public interface OptionLogLevel {

  /**
   * Returns the tree logger level.
   */
  LogLevel getLogLevel();

  /**
   * Sets the tree logger level.
   */
  void setLogLevel(LogLevel logLevel);
}