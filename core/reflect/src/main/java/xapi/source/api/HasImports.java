package xapi.source.api;

public interface HasImports {

  Iterable<String> getImports();
  boolean hasImports();

}
