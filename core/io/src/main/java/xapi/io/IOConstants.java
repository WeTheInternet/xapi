package xapi.io;

public final class IOConstants {

  // We prefer smaller constant ints, since the opcode to load small ints are 0-arg
  public final static int METHOD_GET = 0;
  public final static int METHOD_HEAD = 1;
  public final static int METHOD_POST = 2;
  public final static int METHOD_PUT = 3;
  public final static int METHOD_DELETE = 4;
  public final static int METHOD_PATCH = 5;

  private IOConstants() {}

}
