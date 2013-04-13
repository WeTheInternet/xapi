package xapi.annotation.gc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to prevent a given field from being chain-deleted.
 * 
 * If your children have references to their parents, you may want to chain-delete
 * everything but the parent field (or any other shared data).
 * 
 * This annotation causes a given field to be skipped.
 * 
 * Chain-delete is disabled by default.
 * See {@link OnGC#chainDeleteFields()}
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Indestructible {
}
