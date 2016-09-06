package xapi.args;

import xapi.fu.Out1;

/**
 * Base class for command line argument handlers.
 *
 * @author GWT team "gwtproject.org"
 * @author James X. Nelson "james@wetheinter.net"
 *
 */
public abstract class ArgHandler {

  protected static final String[] EMPTY = new String[0];

  public Out1<String>[] getDefaultArgs() {
    return null;
  }

  public abstract String getPurpose();

  public abstract String getTag();

  /**
   * A list of words representing the arguments in help text.
   */
  public abstract String[] getTagArgs();

  /**
   * Attempts to process one flag or "extra" command-line argument (that appears
   * without a flag).
   * @param args  the arguments passed in to main()
   * @param tagIndex  an index into args indicating the first argument to use.
   * If this is a handler for a flag argument. Otherwise it's the index of the
   * "extra" argument.
   * @return the number of additional arguments consumed, not including the flag or
   * extra argument. Alternately, returns -1 if the argument cannot be used. This will
   * causes the program to abort and usage to be displayed.
   */
  public abstract int handle(String[] args, int tagIndex);

  public int handle(Out1<String>[] args, int tagIndex) {
    String[] resolved = Out1.resolve(String[]::new, args);
    return handle(resolved, tagIndex);
  }

  public boolean isRequired() {
    return false;
  }

  public boolean isUndocumented() {
    return false;
  }

}
