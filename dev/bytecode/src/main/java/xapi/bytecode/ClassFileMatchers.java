package xapi.bytecode;

import java.lang.annotation.Annotation;

import xapi.source.X_Source;
import xapi.util.api.MatchesValue;

public class ClassFileMatchers {

  public static class HasAnnotationMatcher implements MatchesValue<ClassFile>{

    private final String[] annotations;

    @SuppressWarnings("unchecked")
    public HasAnnotationMatcher(Class<? extends Annotation> ... classes) {
      this(X_Source.toString(classes));
    }
    public HasAnnotationMatcher(String ... annotations) {
      this.annotations = annotations;
    }
    
    @Override
    public boolean matches(ClassFile value) {
      for (String annotation : annotations) {
        if (value.getAnnotation(annotation) != null)
          return true;
      }
      return false;
    }
    
  }
  
}
