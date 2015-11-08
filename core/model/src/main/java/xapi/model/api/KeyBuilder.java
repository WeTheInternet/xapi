package xapi.model.api;

import xapi.model.X_Model;
import xapi.util.api.ProvidesValue;

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
  // For types that are application-wide singletons, the default key name is "_theOne_".
  private String id = ModelKey.DEFAULT_ID;

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
    this.parent = new KeyBuilder().withParent(parent);
    return this;
  }

  public ModelKey buildKey() {
    return X_Model.newKey(namespace, kind, id);
  }

  public ModelKey buildKey(String id) {
    return X_Model.newKey(namespace, kind, id);
  }

  public void fromKey(ModelKey key) {
    this.namespace = key.getNamespace();
    this.kind = key.getKind();
    this.id = key.getId();
    if (key.getParent() != null) {
      this.parent = newKeyBuilder(key);
      this.parent.fromKey(key);
    }
  }

  protected KeyBuilder newKeyBuilder(ModelKey key) {
    // let subclasses override this
    return new KeyBuilder();
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

  public static ProvidesValue<KeyBuilder> forType(final String type) {
    return new ProvidesValue<KeyBuilder>() {
      @Override
      public KeyBuilder get() {
        return KeyBuilder.build(type);
      }
    };
  }
}
