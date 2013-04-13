package xapi.source.api;

public interface HasGenerics {

  Iterable<IsGeneric> getGenerics();
  IsGeneric getGeneric(String name);
  boolean hasGenerics();

}
