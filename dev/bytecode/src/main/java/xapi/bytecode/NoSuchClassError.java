package xapi.bytecode;

public class NoSuchClassError extends Error {
	private static final long serialVersionUID = -5000630756875726769L;
private String className;

  /**
   * Constructs an exception.
   */
  public NoSuchClassError(String className, Error cause) {
      super(cause.toString(), cause);
      this.className = className;
  }

  /**
   * Returns the name of the class not found.
   */
  public String getClassName() {
      return className;
  }
}
