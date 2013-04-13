package xapi.model.api;


public interface ModelKey {

  String getNamespace();
  String getKind();
  String getName();
  long getId();

  ModelKey getParent();
  boolean isComplete();

  ModelKey getChild(String kind, long id);
  ModelKey getChild(String kind, String id);


}
