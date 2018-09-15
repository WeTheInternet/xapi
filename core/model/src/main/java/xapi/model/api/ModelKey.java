package xapi.model.api;

import xapi.fu.api.DoNotOverride;

public interface ModelKey {

  String DEFAULT_NAMESPACE = "";
  String DEFAULT_ID = "_theOne_"; // for application-wide singletons
  String DEFAULT_KIND = "_model_";

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

  ModelKey setParent(ModelKey parent);

  boolean isComplete();

  @DoNotOverride
  default boolean isIncomplete() {
    return !isComplete();
  }

  ModelKey getChild(String kind, String id);

  default ModelKey getChild(ModelKey child) {
    return getChild(child.getKind(), child.getId());
  }

}
