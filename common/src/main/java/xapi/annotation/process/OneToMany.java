package xapi.annotation.process;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A process annotation which fans out one signal into many.
 *
 * To receive a signal after all of the Many signals are processed,
 * use a ManyToOne with a stage after the OneToMany.
 *
 * To receive a signal after each item of the Many signals are processed,
 * use a OneToOne (or another OneToMany) with a stage after the OneToMany.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OneToMany {

  int stage();

}
