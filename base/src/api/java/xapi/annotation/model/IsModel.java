package xapi.annotation.model;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import xapi.annotation.mirror.MirroredAnnotation;

/**
 * The annotation used to trigger the model generator.
 *
 * This can be placed on an interface that extends {@link xapi.model.api.Model},
 * to get the full persistence layer support, or on any interface or abstract class
 * to have X_Model.create() produce a filled in, fully functional subclass
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@Documented
@Target({TYPE, PACKAGE})
//@Retention(RetentionPolicy.CLASS)
@Retention(RetentionPolicy.RUNTIME)
@MirroredAnnotation // We want to generate accessor classes for this annotation.
public @interface IsModel {

  String NAMESPACE = "model";

  Key key() default @Key("id");

  Persistent persistence() default @Persistent;

  Serializable serializable() default @Serializable;

  String modelType();

  String[] propertyOrder() default {};

  /**
   * @return a class for a ui component that this model represents.
   * The default return value is IsModel.class, which means "not for a component".
   *
   * This will primarily be defined by generated code for components,
   * however it is exposed here, so that you can signal to the ui
   * generators that the annotated model can be supplied to a component's builder.
   */
  Class<?> forComponent() default IsModel.class;
}
