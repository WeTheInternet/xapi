package xapi.log.api;

import xapi.args.ArgHandler;
import xapi.fu.Out1;

/**
 * Argument handler for processing the log level flag.
 *
 * Based on original implementation from the GWT project.
 *
 * @author GWT team "gwtproject.org"
 * @author James X. Nelson "james@wetheinter.net"
 *
 */
public class ArgHandlerLogLevel extends ArgHandler {

  private static final String OPTIONS_STRING = computeOptionsString();

  private static String computeOptionsString() {
    StringBuffer sb = new StringBuffer();
    LogLevel[] values = LogLevel.values();
    for (int i = 0, c = values.length; i < c; ++i) {
      if (i > 0) {
        sb.append(", ");
      }
      if (i + 1 == c) {
        sb.append("or ");
      }
      sb.append(values[i].name());
    }
    return sb.toString();
  }

  private final OptionLogLevel options;

  public ArgHandlerLogLevel(OptionLogLevel options) {
    this.options = options;
  }

  @Override
  @SuppressWarnings("unchecked")
  public Out1<String>[] getDefaultArgs() {
    return new Out1[] {
        this::getTag, ()->getDefaultLogLevel().name()
    };
  }

  @Override
  public String getPurpose() {
    return "The level of logging detail: " + OPTIONS_STRING;
  }

  @Override
  public String getTag() {
    return "-logLevel";
  }

  @Override
  public String[] getTagArgs() {
    return new String[] {"level"};
  }

  @Override
  public int handle(String[] args, int startIndex) {
    if (startIndex + 1 < args.length) {
      try {
        LogLevel level = LogLevel.valueOf(args[startIndex + 1]);
        options.setLogLevel(level);
        return 1;
      } catch (IllegalArgumentException e) {
        // Argument did not match any enum value; fall through to error case.
      }
    }

    System.err.println(getTag() + " should be followed by one of");
    System.err.println("  " + OPTIONS_STRING);
    return -1;
  }

  protected LogLevel getDefaultLogLevel() {
    return LogLevel.INFO;
  }
}
