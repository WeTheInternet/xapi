package java.lang;
/**
 * Thrown when gwt tries to instantiate a class from incompatible foreign-compiled source code 
 * 
 * @author James X. Nelson (james@wetheinter.net, @james)
 *
 */
public
class ClassFormatError extends LinkageError {
    /**
     * Constructs a <code>ClassFormatError</code> with no detail message. 
     */
    public ClassFormatError() {
  super();
    }

    /**
     * Constructs a <code>ClassFormatError</code> with the specified 
     * detail message. 
     *
     * @param   s   the detail message.
     */
    public ClassFormatError(String s) {
  super(s);
    }
}
