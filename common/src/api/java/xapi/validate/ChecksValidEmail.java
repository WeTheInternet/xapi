package xapi.validate;

public class ChecksValidEmail implements ValidatesValue<String> {

  public static final ChecksValidEmail SINGLETON = new ChecksValidEmail();

  @Override
  public String validate(String value, String errorPrefix) {
    if (value == null)
      return "[value cannot be null] "+errorPrefix;
    if (value.length() == 0)
      return "[value cannot be empty] "+errorPrefix;
    int ind = value.indexOf('@');
    errorPrefix = "You sent: "+value;
    if (ind == -1)
      return "[value must contain @] +"+errorPrefix;
    if (ind == 0)
      return "[value cannot start with @] "+errorPrefix;
    if (!value.substring(0, ind).matches("[a-z0-9._-]+")) {
      return "[username can only contain letter, numbers and . _ or - ] "+errorPrefix;
    }
    value = value.substring(ind + 1);
    if (!value.matches("[a-z0-9.-]+")) {
      return "[domain name can only contain letter, numbers and . or - ] "+errorPrefix;
    }
    if (value.indexOf('.')==-1) {
      return "[domain name must contain a . ] "+errorPrefix;
    }
    return null;
  }

}
