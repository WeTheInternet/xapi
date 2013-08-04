
package java.lang;

/**
 * Subclasses of <code>LinkageError</code> indicate that a class has 
 * some dependency on another class; however, the latter class has 
 * incompatibly changed after the compilation of the former class. 
 *
 *
 * @author  Frank Yellin
 * @version %I%, %G%
 * @since   JDK1.0
 */
//Here for source compatibility
//We may use this in gwt when hotswapping,
//to denote when the recompiled code cannot be bound to live instances.
public class LinkageError extends Error {
    /**
     * Constructs a <code>LinkageError</code> with no detail message. 
     */
    public LinkageError() {
  super();
    }

    /**
     * Constructs a <code>LinkageError</code> with the specified detail 
     * message. 
     *
     * @param   s   the detail message.
     */
    public LinkageError(String s) {
  super(s);
    }
}
