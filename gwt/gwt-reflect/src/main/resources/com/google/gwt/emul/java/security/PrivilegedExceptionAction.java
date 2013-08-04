package java.security;


/**
 * This exception type is used to wrap any Exception thrown while using
 {@link AccessController#doPrivileged(PrivilegedExceptionAction)}
 */

public interface PrivilegedExceptionAction<T> {
    /**
     * Performs the computation.  This method will be called by
     * <code>AccessController.doPrivileged</code> after enabling privileges.
     *
     * @return a class-dependent value that may represent the results of the
     *         computation.  Each class that implements
     *         <code>PrivilegedExceptionAction</code> should document what
     *         (if anything) this value represents.
     * @throws Exception an exceptional condition has occurred.  Each class
     *         that implements <code>PrivilegedExceptionAction</code> should
     *         document the exceptions that its run method can throw.
     * @see AccessController#doPrivileged(PrivilegedExceptionAction)
     * @see AccessController#doPrivileged(PrivilegedExceptionAction,AccessControlContext)
     */

    T run() throws Exception;
}

