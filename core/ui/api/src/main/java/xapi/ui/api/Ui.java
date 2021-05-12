package xapi.ui.api;

import xapi.annotation.mirror.MirroredAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/18/16.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@MirroredAnnotation
public @interface Ui {

  String javaPrefix() default "";

  String type() default "";

  String[] value();

}
