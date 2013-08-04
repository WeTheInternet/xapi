package java.lang.annotation;

/**
 * Source compatibility for runtime Annotation objects.
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
public interface Annotation {
    
  //Copied from java.lang.annotation.Annotation for api compatibility
  
    boolean equals(Object obj);

    int hashCode();

    String toString();

    Class<? extends Annotation> annotationType();
}
