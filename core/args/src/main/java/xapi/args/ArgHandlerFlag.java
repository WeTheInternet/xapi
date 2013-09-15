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

  public abstract boolean setFlag();
}
