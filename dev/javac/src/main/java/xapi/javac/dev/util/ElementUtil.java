package xapi.javac.dev.util;

import javax.lang.model.AnnotatedConstruct;
import java.lang.annotation.Annotation;
import java.util.function.Predicate;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 3/13/16.
 */
public class ElementUtil {
  private ElementUtil(){}

  public static <T extends AnnotatedConstruct> Predicate<T> withAnnotation(Class<? extends Annotation> cls) {
    return annotated->annotated.getAnnotation(cls) != null;
  }
}
