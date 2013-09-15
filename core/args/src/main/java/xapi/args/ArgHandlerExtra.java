package xapi.args;

/**
 * Argument handler that handles arguments that do not have preceeding flags.
 * 
 * Based on original implementation from the GWT project.
 *  
 * @author GWT team "gwtproject.org"
 * @author James X. Nelson "james@wetheinter.net"
 *
 */
public abstract class ArgHandlerExtra extends ArgHandler {

  /**
   * Processes the given "extra" argument.
   * @return false to abort the command and print a usage error.
   */
  public abstract boolean addExtraArg(String arg);

  @Override
  public final String getTag() {
    return null;
  }

  @Override
  public int handle(String[] args, int startIndex) {
    if (addExtraArg(args[startIndex])) {
      return 0;
    } else {
      return -1;
    }
  }

  @Override
  public boolean isRequired() {
    return false;
  }

}
