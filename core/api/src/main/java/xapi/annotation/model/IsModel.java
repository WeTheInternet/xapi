package xapi.annotation.model;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import xapi.annotation.reflect.MirroredAnnotation;

/**
 * The annotation used to trigger the model generator.
 * 
 * This can be placed on an interface that extends {@link xapi.IsModel.api.Model},
 * to get the full persistence layer support, or on any interface or abstract class
 * to have X_Model.create() produce a filled in, fully functional subclass
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@Documented
@Target({TYPE, PACKAGE})
@Retention(RetentionPolicy.CLASS)
@MirroredAnnotation // We want to generate accessor classes for this annotation.
public @interface IsModel {

  Key key() default @Key("id");
  
  Persistent persistence() default @Persistent;
  
  Serializable serializable() default @Serializable;
  
}
