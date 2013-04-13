package java.lang.reflect;

import java.lang.annotation.Annotation;

/**
 * 
 * Emulation layer compatibility for AnnotatedElement
 * 
 * @originalauthor Josh Bloch
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public interface AnnotatedElement {
    /**
     * Returns true if an annotation for the specified type
     * is present on this element, else false.
     */
     boolean isAnnotationPresent(Class<? extends Annotation> annotationClass);

   /**
     * Returns this element's annotation for the specified type if
     * such an annotation is present, else null.
     */
    <T extends Annotation> T getAnnotation(Class<T> annotationClass);

    /**
     * Returns all annotations present on this element.  (Returns an array
     * of length zero if this element has no annotations.) 
     */
    Annotation[] getAnnotations();

    /**
     * Returns all annotations that are directly present on this
     * element.  Unlike the other methods in this interface, this method
     * ignores inherited annotations.  (Returns an array of length zero if
     * no annotations are directly present on this element.)  The caller of
     * this method is free to modify the returned array; it will have no
     * effect on the arrays returned to other callers.
     *
     */
    Annotation[] getDeclaredAnnotations();
}
