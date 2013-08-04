
package java.lang;

/**
 * Thrown if an application tries to call a specified method of a 
 * class (either static or instance), and that class no longer has a 
 * definition of that field. 
 *
 * @author  unascribed
 * @version %I%, %G%
 * @since   JDK1.0
 */
public
class NoSuchFieldError extends IncompatibleClassChangeError {
    /**
     * Constructs a <code>NoSuchFieldError</code> with no detail message.
     */
    public NoSuchFieldError() {
  super();
    }

    /**
     * Constructs a <code>NoSuchFieldError</code> with the 
     * specified detail message. 
     *
     * @param   s   the detail message.
     */
    public NoSuchFieldError(String s) {
  super(s);
    }
}
