package xapi.bytecode;

public class CannotCompileException extends Exception {
	private static final long serialVersionUID = -538765117276975208L;
	private Throwable myCause;

    /**
     * Gets the cause of this throwable.
     * It is for JDK 1.3 compatibility.
     */
    @Override
    public Throwable getCause() {
        return (myCause == this ? null : myCause);
    }

    /**
     * Initializes the cause of this throwable.
     * It is for JDK 1.3 compatibility.
     */
    @Override
    public synchronized Throwable initCause(Throwable cause) {
        myCause = cause;
        return this;
    }

    private String message;

    /**
     * Gets a long message if it is available.
     */
    public String getReason() {
        if (message != null)
            return message;
        else
            return this.toString();
    }

    /**
     * Constructs a CannotCompileException with a message.
     *
     * @param msg       the message.
     */
    public CannotCompileException(String msg) {
        super(msg);
        message = msg;
        initCause(null);
    }

    /**
     * Constructs a CannotCompileException with an <code>Exception</code>
     * representing the cause.
     *
     * @param e     the cause.
     */
    public CannotCompileException(Throwable e) {
        super("by " + e.toString());
        message = null;
        initCause(e);
    }

    /**
     * Constructs a CannotCompileException with a detailed message
     * and an <code>Exception</code> representing the cause.
     *
     * @param msg   the message.
     * @param e     the cause.
     */
    public CannotCompileException(String msg, Throwable e) {
        this(msg);
        initCause(e);
    }

    /**
     * Constructs a CannotCompileException with a
     * <code>NotFoundException</code>.
     */
    public CannotCompileException(NotFoundException e) {
        this("cannot find " + e.getMessage(), e);
    }

//    /**
//     * Constructs a CannotCompileException with an <code>CompileError</code>.
//     */
//    public CannotCompileException(CompileError e) {
//        this("[source error] " + e.getMessage(), e);
//    }

    /**
     * Constructs a CannotCompileException
     * with a <code>ClassNotFoundException</code>.
     */
    public CannotCompileException(ClassNotFoundException e, String name) {
        this("cannot find " + name, e);
    }

    /**
     * Constructs a CannotCompileException with a ClassFormatError.
     */
    public CannotCompileException(ClassFormatError e, String name) {
        this("invalid class format: " + name, e);
    }
}
