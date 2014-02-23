package java.lang;

/**
 * Thrown when a particular method cannot be found.
 */
public
class NoSuchMethodException extends ReflectiveOperationException {
    private static final long serialVersionUID = 5034388446362600923L;

    public NoSuchMethodException() {
        super();
    }

    public NoSuchMethodException(String s) {
        super(s);
    }
}
