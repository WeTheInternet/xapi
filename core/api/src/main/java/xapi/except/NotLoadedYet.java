package xapi.except;

public class NotLoadedYet extends RuntimeException{

  private static final long serialVersionUID = -2405669874730244075L;
  private Class<?> classNotLoaded;

  public NotLoadedYet(String message) {
    super(message);
  }
  public NotLoadedYet(Class<?> cls, String message) {
    super(message);
    classNotLoaded = cls;
  }
  /**
   * @return the classNotLoaded
   */
  public Class<?> getClassNotLoaded() {
    return classNotLoaded;
  }
}
