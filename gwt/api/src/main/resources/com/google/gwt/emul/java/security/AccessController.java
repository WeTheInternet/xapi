package java.security;

/**
 * <p> The AccessController class is used in jre runtimes 
 * for access control operations; in gwt, this class is a no-op;
 * everything is permitted, and this is only here for source compatibility
 */


public final class AccessController {

    /**
     * Don't allow anyone to instantiate an AccessController
     */
    private AccessController() { }

    public static <T> T doPrivileged(PrivilegedAction<T> action){
      return action.run();
    }

    public static AccessControlContext getContext()
    {
        return new AccessControlContext();
    }


    public static <T> T
        doPrivileged(PrivilegedExceptionAction<T> action)
        throws PrivilegedActionException {
        try{
          return action.run();
        } catch (Exception e) {
          throw new PrivilegedActionException(e);
        }
    }

}
