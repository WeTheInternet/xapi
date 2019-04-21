package xapi.annotation.process;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for methods which are called after a OneToMany operation.
 *
 * Methods annotated with ManyToOne will one be called once,
 * after all operations in the "many fanout" are finished.
 *
 * If you wish to be notified of each item on fanout,
 * use a {@link OneToOne} with the same stage as the ManyToOne.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ManyToOne {

  int stage();

}
