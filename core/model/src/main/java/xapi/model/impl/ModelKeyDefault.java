package xapi.model.impl;


import java.util.Objects;

import xapi.annotation.inject.InstanceDefault;
import xapi.model.X_Model;
import xapi.model.api.ModelKey;

@InstanceDefault(implFor=ModelKey.class)
public class ModelKeyDefault implements ModelKey{

  public ModelKeyDefault(final String namespace, final String kind) {
    assert kind != null : "Model Key must NEVER have a null kind";
    this.namespace = namespace == null ? "" : namespace;
    this.kind = kind;
  }

  public ModelKeyDefault(final String namespace, final String kind, final String id) {
    this(namespace, kind);
    this.id = id;
  }

  private ModelKey parentKey;
  private final String kind;

  private final String namespace;
  private String id;
  private int keyType;

  @Override
  public String getNamespace() {
    return namespace;
  }

  @Override
  public String getKind() {
    return kind;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public ModelKey getParent() {
    return parentKey;
  }

  @Override
  public boolean isComplete() {
    return id!=null && !id.isEmpty();
  }

  @Override
  public ModelKey getChild(final String kind, final String id) {
    final ModelKeyDefault key = new ModelKeyDefault(namespace, kind, id);
    key.parentKey = this;
    return key;
  }

  @Override
  public ModelKey setId(final String id) {
    this.id = id;
    return this;
  }

  @Override
  public String toString() {
    return X_Model.keyToString(this);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof ModelKey) {
      final ModelKey asKey = (ModelKey) obj;
      if (!Objects.equals(getNamespace(), asKey.getNamespace())) {
        return false;
      }
      if (!Objects.equals(getKind(), asKey.getKind())) {
        return false;
      }
      if (!Objects.equals(getId(), asKey.getId())) {
        return false;
      }
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = 37;
    if (namespace != null) {
      hash = hash + namespace.hashCode();
    }
    hash = hash ^ ~kind.hashCode();
    if (id != null) {
      hash += id.hashCode();
    }
    return hash;
  }

  @Override
  public int getKeyType() {
    return keyType;
  }

  @Override
  public ModelKeyDefault setKeyType(final int keyType) {
    this.keyType = keyType;
    return this;
  }
}
