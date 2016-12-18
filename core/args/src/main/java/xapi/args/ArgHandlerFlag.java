package xapi.args;

/**
 * Argument handler for flags that have no parameters.
 *
 * Based on original implementation from the GWT project.
 *
 * @author GWT team "gwtproject.org"
 * @author James X. Nelson "james@wetheinter.net"
 *
 */
public abstract class ArgHandlerFlag extends ArgHandler {

  @Override
  public String[] getTagArgs() {
    return EMPTY;
  }

  @Override
  public int handle(String[] args, int startIndex) {
    if (setFlag()) {
      return 0;
    } else {
      return -1;
    }
  }

  @Override
  public boolean isRequired() {
    return false;
  }

  /**
   * Notification that the flag argument was present.
   *
   * @return false if you want to fail the argument processing.
   * If you return false, be sure to log something useful
   */
  public abstract boolean setFlag();

}
