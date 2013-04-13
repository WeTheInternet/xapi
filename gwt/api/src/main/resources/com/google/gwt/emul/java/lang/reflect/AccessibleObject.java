package java.lang.reflect;

import java.security.AccessController;
import sun.reflect.ReflectionFactory;
import java.lang.annotation.Annotation;

/**
 * The AccessibleObject class is the base class for Field, Method and
 * Constructor objects.  It provides the ability to flag a reflected
 * object as suppressing default Java language access control checks
 * when it is used.  The access checks--for public, default (package)
 * access, protected, and private members--are performed when Fields,
 * Methods or Constructors are used to set or get fields, to invoke
 * methods, or to create and initialize new instances of classes,
 * respectively.
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
    public void setAccessible(boolean flag) /* throws SecurityException */ {
      //provided only for emulated compatibility.
      //everything is accessible in gwt.
    }

    /**
     * All objects are accessible in gwt.
     */
    public boolean isAccessible() {
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