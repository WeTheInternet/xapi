
package java.lang;

/**
 * Here for source compatibility
 */
public
class NoClassDefFoundError extends LinkageError {
    /**
     * Constructs a <code>NoClassDefFoundError</code> with no detail message.
     */
    public NoClassDefFoundError() {
  super();
    }

    /**
     * Constructs a <code>NoClassDefFoundError</code> with the specified 
     * detail message. 
     *
     * @param   s   the detail message.
     */
    public NoClassDefFoundError(String s) {
  super(s);
    }
}
