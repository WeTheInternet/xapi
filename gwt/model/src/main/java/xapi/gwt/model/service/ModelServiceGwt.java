package xapi.gwt.model.service;

import com.google.gwt.core.client.GWT;

import xapi.annotation.inject.SingletonOverride;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.StringDictionary;
import xapi.dev.source.CharBuffer;
import xapi.except.NotConfiguredCorrectly;
import xapi.io.X_IO;
import xapi.io.api.DelegatingIOCallback;
import xapi.log.X_Log;
import xapi.model.X_Model;
import xapi.model.api.Model;
import xapi.model.api.ModelKey;
import xapi.model.api.ModelSerializer;
import xapi.model.api.PrimitiveSerializer;
import xapi.model.impl.AbstractModelService;
import xapi.model.impl.ModelSerializerDefault;
import xapi.model.service.ModelService;
import xapi.platform.GwtPlatform;
import xapi.source.impl.StringCharIterator;
import xapi.util.api.ProvidesValue;
import xapi.util.api.SuccessHandler;

@GwtPlatform
@SuppressWarnings({"rawtypes"})
@SingletonOverride(implFor=ModelService.class)
public class ModelServiceGwt extends AbstractModelService
{


  @SuppressWarnings("unchecked")
  private static ClassTo<ProvidesValue<? extends Model>> PROVIDERS = X_Collect.newClassMap(
      Class.class.cast(ProvidesValue.class)
  );

  public static String REGISTER_CREATOR_METHOD = "registerCreator";

  private static Class<? extends Model> implClassRef;
  public static <M extends Model> String registerCreator(final Class<M> cls, final Class<? extends M> implClass, final ProvidesValue<M> provider) {
    PROVIDERS.put(cls, provider);
    implClassRef = implClass;
    final String type = X_Model.getService().register(cls);
    implClassRef = null;
    return type;
  }

  @Override
  public <T extends Model> T create(final Class<T> key) {
    final ProvidesValue<? extends Model> provider = PROVIDERS.get(key);
    if (provider == null) {
      throw new NotConfiguredCorrectly("Could not find provider for "+key+"; did you forget to call X_Model.register()?");
    }
    if (!classToTypeName.containsKey(key)) {
      final T model = (T) provider.get();
      classToTypeName.put(key, model.getType());
      return model;
    }
    return (T) provider.get();
  }

  /**
   * @see xapi.model.impl.AbstractModelService#getDefaultSerializer(java.lang.Class)
   */
  @Override
  protected <M extends Model> ModelSerializer getDefaultSerializer(final String typeName) {

    return new ModelSerializerDefault<Model>() {
      @Override
      protected boolean isModelType(final Class<?> propertyType) {
        return PROVIDERS.containsKey(propertyType);
      }
    };
  }

  @Override
  public String register(final Class<? extends Model> model) {
    super.register(model);
    final String type = classToTypeName.get(model);
    if (implClassRef != null) {
      if (type != null) {
        classToTypeName.put(implClassRef, type);
      }
    }
    return type;
  }

  @Override
  protected <M extends Model> void doPersist(final String type, final M model, final SuccessHandler<M> callback) {
    final String url = getUrlBase()+"model/persist";
    final StringDictionary<String> headers = X_Collect.newDictionary();
    headers.setValue("X-Model-Type", model.getType());
    final CharBuffer serialized =  serialize(type, model);
    X_IO.getIOService().post(url, serialized.toString(), headers, new DelegatingIOCallback<>((resp) -> {
      final M deserialized = deserialize(type, new StringCharIterator(resp.body()));
      callback.onSuccess(deserialized);
    }));
  }

  protected String getUrlBase() {
    return GWT.getHostPageBaseURL();
  }

  @Override
  public <M extends Model> void load(final Class<M> type, final ModelKey modelKey, final SuccessHandler<M> callback) {
    final String url = getUrlBase()+"model/load";
    final String typeName = getTypeName(type);
    final StringDictionary<String> headers = X_Collect.newDictionary();
    headers.setValue("X-Model-Type", typeName);
    final PrimitiveSerializer primitives = primitiveSerializer();
    String ns = modelKey.getNamespace().length() == 0 ? "" : modelKey.getNamespace();
    // The namespace might be empty, so we use the serialized form that can transmit "" without
    // constructing an invalid uri.
    ns = primitives.serializeString(ns);
    final String kind = modelKey.getKind();
    final String id = primitives.serializeInt(modelKey.getKeyType()) + modelKey.getId();
    final String serialized = "/" + ns + "/"+kind+"/"+id;
    X_IO.getIOService().get(url+""+serialized, headers, new DelegatingIOCallback<>(resp -> {
      X_Log.error("Got response! "+resp.body());
      final M deserialized = deserialize(type, new StringCharIterator(resp.body()));
      callback.onSuccess(deserialized);
    }));

  }

  @Override
  protected boolean isClientToServer() {
    return true;
  }

}
