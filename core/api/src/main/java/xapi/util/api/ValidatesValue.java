package xapi.util.api;

public interface ValidatesValue <V> {

  /**
   * Return null for valid,
   * and String message for invalid.
   * @param object - The object to validate
   * @param message - Some debug data to prepend to invalid message
   * @return message + " why object is invalid";
   */
  String validate(V object, String message);

}
