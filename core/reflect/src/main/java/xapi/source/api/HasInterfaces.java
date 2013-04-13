package xapi.source.api;

public interface HasInterfaces {

  Iterable<IsClass> getInterfaces();
  boolean hasInterface();

}
