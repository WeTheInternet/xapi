package xapi.model.api;


public interface ModelKey {

  int KEY_TYPE_STRING = 0;
  int KEY_TYPE_LONG = 1;
  // All other ints are usable to denote custom key types that end users may
  // embed as serialized strings in the id field.  All model service transactions
  // will use the serialized String id format, but backends may need to understand
  // when to convert the key types; long ids, for example, are used by appengine.

  String getNamespace();
  String getKind();
  String getId();
  int getKeyType();
  ModelKey setKeyType(int keyType);

  ModelKey setId(String id);

  ModelKey getParent();
  boolean isComplete();

  ModelKey getChild(String kind, String id);


}
