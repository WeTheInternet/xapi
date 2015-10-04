package xapi.model.api;

import xapi.collect.X_Collect;
import xapi.collect.api.StringDictionary;
import xapi.collect.api.StringTo;
import xapi.util.api.ReceivesValue;

import java.util.function.Supplier;

/**
 * Created by james on 03/10/15.
 */
public class ModelBuilder <M extends Model> {
  private KeyBuilder key = new KeyBuilder();
  private StringDictionary<Object> properties = X_Collect.newDictionary();
  private Supplier<M> creator;

  public static <M extends Model> ModelBuilder<M> build(Supplier<M> creator) {
    ModelBuilder builder = new ModelBuilder();
    builder.creator = creator;
    return builder;
  }

  public static <M extends Model> ModelBuilder<M> build(ModelKey withKey, Supplier<M> creator) {
    ModelBuilder builder = new ModelBuilder();
    builder.key.fromKey(withKey);
    builder.creator = creator;
    return builder;
  }

  public static <M extends Model> ModelBuilder<M> build(KeyBuilder withBuilder, Supplier<M> creator) {
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

  public ModelBuilder<M> withCreator(Supplier<M> creator) {
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

  public ModelBuilder withProperty(String propName, Object value) {
    properties.setValue(propName, value);
    return this;
  }

  public ModelBuilder withParent(String propName, ModelKey parent) {
    key.withParent(parent);
    return this;
  }

  public ModelBuilder withParent(String propName, KeyBuilder parent) {
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
    final M model = creator.get();
    return model;
  }

  public M buildModel(Supplier<M> creator) {
    Supplier<M> oldCreator = creator;
    this.creator = creator;
    M model = buildModel();
    this.creator = oldCreator;
    oldCreator = null;
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
