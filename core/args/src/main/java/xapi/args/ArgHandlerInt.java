package xapi.args;

/**
 * Argument handler for flags that take an integer as their parameter. 
 * 
 * Based on original implementation from the GWT project.
 *  
 * @author GWT team "gwtproject.org"
 * @author James X. Nelson "james@wetheinter.net"
 *
 */
public abstract class ArgHandlerInt extends ArgHandler {

  @Override
  public int handle(String[] args, int startIndex) {
    int value;
    if (startIndex + 1 < args.length) {
      try {
        value = Integer.parseInt(args[startIndex + 1]);
      } catch (NumberFormatException e) {
        // fall-through
        value = -1;
      }

      setInt(value);
      return 1;
    }

    System.err.println(getTag() + " should be followed by an integer");
    return -1;
  }

  @Override
  public boolean isRequired() {
    return false;
  }

  public abstract void setInt(int value);
}
