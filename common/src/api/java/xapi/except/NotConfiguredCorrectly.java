package xapi.except;

public class NotConfiguredCorrectly extends Error{

  public static final String SUPER_BELOW_GWT_DEV =
    "You appear to have xapi-super below gwt-dev on your classpath";

  private static final long serialVersionUID = -2405669874730244075L;

  public NotConfiguredCorrectly(String message) {
    super(message);
  }
}
