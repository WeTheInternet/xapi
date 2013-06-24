package xapi.source.api;

public interface HasAnnotations {

  Iterable<IsAnnotation> getAnnotations();
  IsAnnotation getAnnotation(String name);

}
