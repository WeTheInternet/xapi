package xapi.bytecode;

import xapi.bytecode.annotation.Annotation;

public interface Annotated {

  Annotation[] getAnnotations();
  Annotation getAnnotation(String annoClassName);
  
}
