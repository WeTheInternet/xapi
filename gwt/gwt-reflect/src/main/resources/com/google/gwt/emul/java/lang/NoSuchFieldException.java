
package java.lang;

/**
 * Thrown if an application tries to access a method of a 
 * class (either static or instance), and that class does not have a 
 * definition of that method. 
 */
public
class NoSuchFieldException extends Exception {
  private static final long serialVersionUID = -6143714805279938260L;
 
  /**
     * Constructs a <code>NoSuchFieldException</code> with no detail message.
     */
    public NoSuchFieldException() {
  super();
    }

    /**
     * Constructs a <code>NoSuchFieldException</code> with the 
     * specified detail message. 
     *
     * @param   s   the detail message.
     */
    public NoSuchFieldException(String s) {
  super(s);
    }
}
