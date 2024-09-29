package xapi.annotation.model;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation specifying that a field or class requires serializing from Model
 * to String, in order to send to the server.
 *
 * This can be applied to interfaces, classes, methods or fields, though using
 * field-level concrete classes breaks compatibility;
 * you should stick to interfaces + methods, and avoid classes + fields.
 *
 * Applying this to the class makes every field in the model serializable on
 * the client, and you will have to use {@link ClientToServer#enabled()} = false
 * to strictly disable any fields.
 *
 * This makes it easy to add client-only fields that can be used on the server,
 * provided your interface is in a shared lib.  If you need to hide client-only
 * fields, manually call .setProperty(String, Object) on your models.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({}) // Must be placed inside a @Serializable annotation
public @interface ServerToClient {

  SerializationStrategy serializer() default SerializationStrategy.ProtoStream;

  boolean enabled() default true;

  boolean encrypted() default false;

}
