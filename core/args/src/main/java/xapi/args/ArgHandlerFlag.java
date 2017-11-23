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

  private boolean wasSet;

  @Override
  public String[] getTagArgs() {
    return EMPTY;
  }

  @Override
  public int handle(String[] args, int startIndex) {
    wasSet = true;
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

  public boolean isSet() {
    return wasSet;
  }

  /**
   * Notification that the flag argument was present.
   *
   * @return false if you want to fail the argument processing.
   * If you return false, be sure to log something useful
   */
  public boolean setFlag() {
    wasSet = true;
    return true;
  }

}
