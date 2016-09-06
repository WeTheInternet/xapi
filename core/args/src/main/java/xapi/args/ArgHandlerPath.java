package xapi.args;

import xapi.fu.Out1;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Argument handler for arguments that are directories.
 *
 * Based on original implementation from the GWT project.
 *
 * @author GWT team "gwtproject.org"
 * @author James X. Nelson "james@wetheinter.net"
 *
 */
public abstract class ArgHandlerPath extends ArgHandler {

  @Override
  public Out1<String>[] getDefaultArgs() {
    return null;
  }

  @Override
  public String[] getTagArgs() {
    return new String[]{"path"};
  }

  @Override
  public int handle(String[] args, int startIndex) {
    if (startIndex + 1 < args.length) {
      setPath(Paths.get(args[startIndex + 1]));
      return 1;
    }

    System.err.println(getTag()
      + " should be followed by the name of a file");
    return -1;
  }

  public abstract void setPath(Path path);

}
