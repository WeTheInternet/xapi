package xapi.model.service;

import xapi.dev.source.CharBuffer;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelManifest;
import xapi.model.api.PrimitiveSerializer;
import xapi.source.api.CharIterator;
import xapi.util.api.SuccessHandler;

public interface ModelService {

  String register(Class<? extends Model> model);
  <T extends Model> T create(Class<T> key);
  void persist(Model model, SuccessHandler<Model> callback);
  <M extends Model> CharBuffer serialize(final Class<M> cls, final M model);
  <M extends Model> CharBuffer serialize(final ModelManifest manifest, final M model);
  <M extends Model> M deserialize(final Class<M> cls, final CharIterator model);
  <M extends Model> M deserialize(final ModelManifest manifest, final CharIterator model);
  PrimitiveSerializer primitiveSerializer();
  String keyToString(ModelKey key);
  ModelKey keyFromString(String key);
  ModelKey newKey(String namespace, String kind);
  ModelKey newKey(String namespace, String kind, String id);
  <M extends Model> void load(Class<M> modelClass, ModelKey modelKey, SuccessHandler<M> callback);

}
