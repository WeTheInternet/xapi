package xapi.except;

public class NotAuthorized extends Exception{

  private static final long serialVersionUID = -4298621052998449105L;

  private boolean fatal;

  protected NotAuthorized() {//serialization constructor
  }

  public NotAuthorized(String reason, boolean fatal) {
    super(reason);
    this.fatal = fatal;
  }

  /**
   * @return Whether this is a fatal authorization exception or not.
   */
  public boolean isFatal() {
    return fatal;
  }

}
