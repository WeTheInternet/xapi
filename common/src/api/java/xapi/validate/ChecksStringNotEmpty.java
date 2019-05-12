package xapi.validate;

public class ChecksStringNotEmpty extends ChecksNonNull{

  public static final ChecksStringNotEmpty SINGLETON = new ChecksStringNotEmpty();

  @Override
  public String validate(Object object, String message) {
    String error = super.validate(object, message);
    if (error != null)
      return error;
    if (String.valueOf(object).trim().length() == 0)
      return "[string value cannot be empty] "+message;
    return null;
  }
  public ChecksStringNotEmpty() {
  }
}
