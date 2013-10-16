package java.lang.reflect;

import java.lang.annotation.Annotation;

/**
 * The AccessibleObject class is the base class for Field, Method and
 * Constructor objects.  It normally provides the ability to flag a reflected
 * object as suppressing default Java language access control checks
 * when it is used.  
 * 
 * In gwt, all objects will be made accessible through jsni
 *
 * <p>Setting the <tt>accessible</tt> flag in a reflected object
 * permits sophisticated applications with sufficient privilege, such
 * as Java Object Serialization or other persistence mechanisms, to
 * manipulate objects in a manner that would normally be prohibited.
 *
 * @see Field
 * @see Method
 * @see Constructor
 * @see ReflectPermission
 *
 * @since 1.2
 */
public class AccessibleObject implements AnnotatedElement {

  /**
   * No-op in gwt; isAccessible returns true.
   */
    public static void setAccessible(AccessibleObject[] array, boolean flag)
    {
      //no-op in gwt.  Here for api compatibility;
    }

    /**
     * No-op in gwt; isAccessible returns true.
     */
    public final void setAccessible(boolean flag) /* throws SecurityException */ {
      //provided only for emulated compatibility.
      //everything is accessible in gwt.
    }

    /**
     * All objects are accessible in gwt.
     */
    public final boolean isAccessible() {
      return true;
    }

    /**
     * Constructor: only used by the Java Virtual Machine.
     */
    protected AccessibleObject() {}

    /**
     * @throws NullPointerException {@inheritDoc}
     * @since 1.5
     */
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        throw new AssertionError("All subclasses should override this method");
    }

    /**
     * @throws NullPointerException {@inheritDoc}
     * @since 1.5
     */
    public boolean isAnnotationPresent(
        Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotationClass) != null;
    }

    /**
     * @since 1.5
     */
    public Annotation[] getAnnotations() { 
        return getDeclaredAnnotations();
    }

    /**
     * @since 1.5
     */
    public Annotation[] getDeclaredAnnotations()  {
        throw new AssertionError("All subclasses should override this method");
    }
}