package xapi.model.api;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType
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

  @JsProperty
  String getNamespace();
  @JsProperty
  String getKind();
  @JsProperty
  String getId();
  @JsProperty
  int getKeyType();

  ModelKey setKeyType(int keyType);

  ModelKey setId(String id);

  @JsProperty
  ModelKey getParent();

  boolean isComplete();

  ModelKey getChild(String kind, String id);


}
