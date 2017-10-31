package xapi.args;

import xapi.fu.Out1;

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
public abstract class ArgHandlerFile extends ArgHandler {

  protected File file;

  @Override
  public Out1<String>[] getDefaultArgs() {
    return null;
  }

  @Override
  public String[] getTagArgs() {
    return new String[]{"file"};
  }

  @Override
  public int handle(String[] args, int startIndex) {
    if (startIndex + 1 < args.length) {
      setFile(new File(args[startIndex + 1]));
      return 1;
    }

    System.err.println(getTag()
      + " should be followed by the name of a file");
    return -1;
  }

  public void setFile(File file) {
    this.file = file;
  }

  public File getFile() {
    return file;
  }
}
