package xapi.annotation.model;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation specifying that a field or class will be sent from the client
 * (but not necessarily back from the server).
 *
 * This annotation can only be applied inside a @{@link Serializable} annotation.
 * 
 * Use @Serializable to mark a field or type as serializable, and @ClientToServer
 * to tweak the configuration of the generated api.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({}) // Must be placed inside an @Serializable field.
public @interface ClientToServer {

  SerializationStrategy serializer() default SerializationStrategy.ProtoStream;

  boolean enabled() default true;

  boolean encrypted() default false;
}
