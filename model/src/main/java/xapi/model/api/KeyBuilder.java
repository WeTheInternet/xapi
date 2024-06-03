package xapi.model.api;

import xapi.fu.Out1;
import xapi.model.X_Model;

/**
 * Created by james on 03/10/15.
 */
public class KeyBuilder {

  public static KeyBuilder build(String namespace, String kind) {
    return new KeyBuilder().withNamespace(namespace).withKind(kind);
  }
  public static KeyBuilder build(String kind) {
    return new KeyBuilder().withNamespace(ModelKey.DEFAULT_NAMESPACE).withKind(kind);
  }

  public static ModelKey createKey(String namespace, String kind, String id) {
    return new KeyBuilder().withNamespace(namespace).withKind(kind).withId(id).buildKey();
  }
  public static ModelKey createKey(String kind, String id) {
    return new KeyBuilder().withNamespace(ModelKey.DEFAULT_NAMESPACE).withKind(kind).withId(id).buildKey();
  }

  private String namespace = ModelKey.DEFAULT_NAMESPACE;
  private String kind = ModelKey.DEFAULT_KIND;
  private String id = "";//ModelKey.DEFAULT_ID;

  private int type = ModelKey.KEY_TYPE_STRING;

  private KeyBuilder parent;

  public KeyBuilder withNamespace(String namespace) {
    assert namespace != null : "Do not set a null namespace";
    this.namespace = namespace;
    return  this;
  }
  public KeyBuilder withKind(String kind) {
    assert kind != null : "Do not set a null kind";
    this.kind = kind;
    return  this;
  }
  public KeyBuilder withId(String id) {
    assert id != null : "Do not set a null id";
    this.id = id;
    return this;
  }

  public KeyBuilder withParent(KeyBuilder parent) {
    this.parent = parent;
    return this;
  }

  public KeyBuilder withParent(ModelKey parent) {
    if (parent == null) {
      this.parent = null;
    } else {
      this.parent = newKeyBuilder(parent);
    }
    return this;
  }

  public KeyBuilder withType(int type) {
    this.type = type;
    return this;
  }

  public ModelKey buildKey() {
    final ModelKey key = X_Model.newKey(namespace, kind, id);
    key.setKeyType(type);
    return key;
  }

  public ModelKey buildKey(String id) {
    final ModelKey key = X_Model.newKey(namespace, kind, id);
    key.setKeyType(type);
    return key;
  }

  public ModelKey buildKeyLong(long id) {
    final ModelKey key = X_Model.newKey(namespace, kind);
    key.setKeyType(ModelKey.KEY_TYPE_LONG);
    key.setId(Long.toString(id));
    return key;
  }

  public KeyBuilder fromKey(ModelKey key) {
    this.namespace = key.getNamespace();
    this.kind = key.getKind();
    this.id = key.getId();
    if (key.getParent() != null) {
      this.parent = newKeyBuilder(key);
    }
    return this;
  }

  protected KeyBuilder newKeyBuilder(ModelKey key) {
    // let subclasses override this
    return new KeyBuilder().fromKey(key);
  }

  public KeyBuilder fromBuilder(KeyBuilder other) {
    this.namespace = other.namespace;
    this.kind = other.kind;
    this.id = other.id;
    if (other.parent != null) {
      this.parent = new KeyBuilder().fromBuilder(other.parent);
    }
    return this;
  }

  public static Out1<KeyBuilder> forType(final String type) {
    return Out1.out1Deferred(KeyBuilder::build, type);
  }
}
