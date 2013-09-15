package xapi.args;

import java.io.File;

/**
 * Argument handler for arguments that are directories.
 * 
 * Based on original implementation from the GWT project.
 *  
 * @author GWT team "gwtproject.org"
 * @author James X. Nelson "james@wetheinter.net"
 *
 */
public abstract class ArgHandlerDir extends ArgHandler {

  @Override
  public String[] getTagArgs() {
    return new String[]{"dir"};
  }

  @Override
  public int handle(String[] args, int startIndex) {
    if (startIndex + 1 < args.length) {
      setDir(new File(args[startIndex + 1]));
      return 1;
    }

    System.err.println(getTag()
      + " should be followed by the name of a directory");
    return -1;
  }

  public abstract void setDir(File dir);

}
