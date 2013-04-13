package java.security;

public interface Guard {

    /**
     * Determines whether or not to allow access to the guarded object
     * <code>object</code>. Returns silently if access is allowed.
     * Otherwise, throws a SecurityException.
     *
     * @param object the object being protected by the guard.
     *
     * @exception SecurityException if access is denied.
     *
     */
    void checkGuard(Object object) throws SecurityException;
}
