package xapi.model.api;

import xapi.collect.X_Collect;
import xapi.collect.api.StringDictionary;
import xapi.fu.Out1;
import xapi.util.api.ReceivesValue;

/**
 * Created by james on 03/10/15.
 */
public class ModelBuilder <M extends Model> {
  private KeyBuilder key = new KeyBuilder();
  private StringDictionary<Object> properties = X_Collect.newDictionary();
  private Out1<M> creator;

  public static <M extends Model> ModelBuilder<M> build(Out1<M> creator) {
    ModelBuilder builder = new ModelBuilder();
    builder.creator = creator;
    return builder;
  }

  public static <M extends Model> ModelBuilder<M> build(ModelKey withKey, Out1<M> creator) {
    ModelBuilder builder = new ModelBuilder();
    builder.key.fromKey(withKey);
    builder.creator = creator;
    return builder;
  }

  public static <M extends Model> ModelBuilder<M> build(KeyBuilder withBuilder, Out1<M> creator) {
    ModelBuilder builder = new ModelBuilder();
    builder.key.fromBuilder(withBuilder);
    builder.creator = creator;
    return builder;
  }

  public KeyBuilder key() {
    return key;
  }

  public ModelBuilder<M> withId(String id) {
    key.withId(id);
    return this;
  }

  public ModelBuilder<M> withCreator(Out1<M> creator) {
    this.creator = creator;
    return this;
  }

  public ModelBuilder<M> withKind(String kind) {
    key.withKind(kind);
    return this;
  }

  public ModelBuilder<M> withNamespace(String namespace) {
    key.withNamespace(namespace);
    return this;
  }

  public ModelBuilder<M> withProperty(String propName, Object value) {
    properties.setValue(propName, value);
    return this;
  }

  public ModelBuilder<M> withParent(String propName, ModelKey parent) {
    key.withParent(parent);
    return this;
  }

  public ModelBuilder<M> withParent(String propName, KeyBuilder parent) {
    key.withParent(parent);
    return this;
  }

  public M buildModel() {
    final M model = newModel();
    model.setKey(key.buildKey());
    copyProperties(model);
    return model;
  }

  private void copyProperties(final M model) {
    properties.forKeys(new ReceivesValue<String>() {
      @Override
      public void set(String id) {
        model.setProperty(id, properties.getValue(id));
      }
    });
  }

  private M newModel() {
    assert creator != null : "You MUST supply a creator to ModelBuilder";
    final M model = creator.out1();
    return model;
  }

  public synchronized M buildModel(Out1<M> creator) {
    Out1<M> oldCreator = creator;
    this.creator = creator;
    M model = buildModel();
    this.creator = oldCreator;
    return model;
  }
  public M buildModel(String id) {
    ModelKey newKey = key.buildKey(id);
    return buildModel(newKey);
  }

  public M buildModel(ModelKey key) {
    final M model = newModel();
    model.setKey(key);
    copyProperties(model);
    return model;
  }
}
