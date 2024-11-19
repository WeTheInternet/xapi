package xapi.ui.autoui.api;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A marker annotation used to tell reflection processors not to index the annotated element.
 * <br>
 * This is used by autoui to exclude methods from bean processing, either because the
 * given element is not supposed to be accessible, or to prevent recursion sickness.
 * 
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface DoNotIndex {

  int unlessDepthLessThan() default 0;
}
