package xapi.ui.autoui.api;

public interface Validator <T> {

  /**
   * @param object -> The object whose validity to check
   * @return -> null or Boolean.TRUE to signify validity.
   * <p>
   * Anything else returned will be treated as a validation error
   * (and will probably be toString()ed into an error message)
   */
  T isValid(Object object);

}
