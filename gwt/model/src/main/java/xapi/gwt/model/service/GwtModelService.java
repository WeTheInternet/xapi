package xapi.gwt.model.service;

import java.util.Objects;

import xapi.annotation.gwt.MagicMethod;
import xapi.annotation.inject.SingletonOverride;
import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.inject.X_Inject;
import xapi.io.X_IO;
import xapi.io.service.IOService;
import xapi.model.api.Model;
import xapi.model.api.ModelSerializer;
import xapi.model.service.ModelService;
import xapi.platform.GwtPlatform;
import xapi.util.api.SuccessHandler;

@GwtPlatform
@SuppressWarnings({"rawtypes", "unchecked"})
@SingletonOverride(implFor=ModelService.class)
public class GwtModelService implements ModelService
{

  private final ClassTo<ModelSerializer> serializers = X_Collect.newClassMap(ModelSerializer.class);
  private final ClassTo<Class> implToInterface = X_Collect.newClassMap(Class.class);

  @Override
  @MagicMethod(doNotVisit=true, documentation="This magic method re-routes to the same provider as X_Inject.instance()")
  public <T extends Model> T create(final Class<T> key) {
    // GWT dev will make it here, and it can handle non-class-literal injection.
    // GWT prod requires magic method injection here.
    final T instance = X_Inject.instance(key);
    implToInterface.put(instance.getClass(), key);
    return instance;
  }

  @Override
  public void persist(final Model model, final SuccessHandler<Model> callback) {
    assert Objects.nonNull(model) : "Cannot persist a null model";
    final Class type = implToInterface.get(model.getClass());
    final String asString = serialize(type, model);
    final IOService ioService = X_IO.getIOService();
  }

  @Override
  public <M extends Model> M deserialize(final Class<M> cls, final String model) {
    if (model == null) {
      return null;
    }
    return getSerializer(cls).modelFromString(model);
  }

  private <M extends Model> ModelSerializer<M> getSerializer(final Class<M> cls) {
    ModelSerializer serializer = serializers.get(cls);
    if (serializer == null) {
      final Class original = implToInterface.get(cls);
      if (original != null) {
        serializer = serializers.get(original);
      }
    }
    if (serializer == null) {
      throw unsupportedOperation(cls);
    }
    return serializer;
  }

  @Override
  public <M extends Model> String serialize(final Class<M> cls, final M model) {
    if (model == null) {
      return null;
    }
    return getSerializer(cls).modelToString(model);
  }

  @Override
  public void register(final Class<? extends Model> model) {
  }

  private final UnsupportedOperationException unsupportedOperation(final Class<?> c) {
    return new UnsupportedOperationException("Unable to find a serializer for the type "+c.getClass()+".");
  }

}
