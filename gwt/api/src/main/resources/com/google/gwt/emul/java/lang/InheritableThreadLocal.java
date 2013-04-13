
package java.lang;

/**
 * TODO:
 * properly support threadlocals beyond a singleton for single-threaded gwt.
 * 
 * Either using "virtual threads" to represent an operation's environment,
 * or other iframe windows / web workers existing as actual separate threads;
 * provided all inter-thread communication is handled with serializable objects,
 * and async interfaces for accessing functionality, it should be possible
 * to build a single unified multi threading api that will work on the client
 * and on the server.
 * 
 * Original documentation for InheritableThreadLocal:
 * 
 * 
 * This class extends <tt>ThreadLocal</tt> to provide inheritance of values
 * from parent thread to child thread: when a child thread is created, the
 * child receives initial values for all inheritable thread-local variables
 * for which the parent has values.  Normally the child's values will be
 * identical to the parent's; however, the child's value can be made an
 * arbitrary function of the parent's by overriding the <tt>childValue</tt>
 * method in this class.
 * 
 * <p>Inheritable thread-local variables are used in preference to
 * ordinary thread-local variables when the per-thread-attribute being
 * maintained in the variable (e.g., User ID, Transaction ID) must be
 * automatically transmitted to any child threads that are created.
 *
 * @author  Josh Bloch and Doug Lea
 * @version %I%, %G%
 * @see     ThreadLocal
 * @since   1.2
 */

public class InheritableThreadLocal<T> extends ThreadLocal<T> {
    /**
     * Computes the child's initial value for this inheritable thread-local
     * variable as a function of the parent's value at the time the child
     * thread is created.  This method is called from within the parent
     * thread before the child is started.
     * <p>
     * This method merely returns its input argument, and should be overridden
     * if a different behavior is desired.
     *
     * @param parentValue the parent thread's value
     * @return the child thread's initial value
     */
    protected T childValue(T parentValue) {
        return parentValue;
    }

//    /**
//     * Get the map associated with a ThreadLocal. 
//     *
//     * @param t the current thread
//     */
//    ThreadLocalMap getMap(Thread t) {
//       return t.inheritableThreadLocals;
//    }

    /**
     * Create the map associated with a ThreadLocal. 
     *
     * @param t the current thread
     * @param firstValue value for the initial entry of the table.
     * @param map the map to store.
     */
    void createMap(Thread t, T firstValue) {
//        t.inheritableThreadLocals = new ThreadLocalMap(this, firstValue);
    }
}
