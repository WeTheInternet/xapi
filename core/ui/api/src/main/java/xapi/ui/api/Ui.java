package xapi.ui.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/18/16.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Ui {

  String type() default "";

  String[] value();

}
