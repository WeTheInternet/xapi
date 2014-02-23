
package java.lang;

/**
 * Common superclass of exceptions thrown by reflective operations reflection.
 */
public class ReflectiveOperationException extends Exception {
    static final long serialVersionUID = 123456789L;

    public ReflectiveOperationException() {
        super();
    }
    public ReflectiveOperationException(String message) {
        super(message);
    }
    public ReflectiveOperationException(String message, Throwable cause) {
        super(message, cause);
    }
    public ReflectiveOperationException(Throwable cause) {
        super(cause);
    }
}
