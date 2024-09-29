package xapi.validate;

public class ChecksNonNull implements ValidatesValue<Object> {

  public static final ChecksNonNull SINGLETON = new ChecksNonNull();

  @Override
  public String validate(Object object, String message) {
    return object == null ? "[value cannot be null] "+message : null;
  }

}
