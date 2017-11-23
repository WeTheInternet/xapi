package xapi.args;

/**
 * Argument handler for processing flags that take a string as their parameter.
 *
 * Based on original implementation from the GWT project.
 *
 * @author GWT team "gwtproject.org"
 * @author James X. Nelson "james@wetheinter.net"
 *
 */
public abstract class ArgHandlerString extends ArgHandler {

  protected String arg;

  @Override
  public int handle(String[] args, int startIndex) {
    if (startIndex + 1 < args.length) {
      arg = args[startIndex + 1];
      if (!setString(arg)) {
        return -1;
      }
      return 1;
    }

    System.err.println(getTag() + " must be followed by an argument for "
      + getTagArgs()[0]);
    return -1;
  }

  public boolean setString(String str) {
    this.arg = str;
    return true;
  }

}
