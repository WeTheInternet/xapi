package xapi.model.api;

import xapi.annotation.model.*;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.fu.In2;
import xapi.fu.api.Ignore;
import xapi.fu.data.SetLike;
import xapi.fu.itr.SizedIterable;
import xapi.fu.itr.SizedIterator;
import xapi.model.X_Model;
import xapi.util.api.SuccessHandler;

import static xapi.collect.X_Collect.copyMap;

/**
 * A model which represents a list of models; stored as a set-like array of keys.
 *
 * This implements SetLike instead of ListLike, because we internally use ModelKey;
 * we may consider a ModelMap v. ModelList type later, to make this a bit clearer
 * (i.e. a list can hold models w/out fully resolved keys).
 *
 * When this is fully supported (it isn't yet), all model that are contained
 * will be saved as sub-entities whenever the list itself is saved, and the
 * keys of those entities will be the only thing persisted into the owning model.
 *
 * For now, this is just a nice way for us to have typesafe lists of models at runtime.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 10/4/18 @ 1:58 AM.
 */

@IsModel(
        modelType = "list"
        ,persistence = @Persistent(strategy= PersistenceStrategy.Inherit)
        ,serializable = @Serializable(
        clientToServer=@ClientToServer(encrypted=true)
        ,serverToClient = @ServerToClient(encrypted=true)
)
)
public interface ModelList<T extends Model> extends Model, SetLike<T> {

    String MODEL_LIST = "n";

    /**
     * Yes, we want to record the class literal of the model type;
     * it's our key to let us easily use otherwise-overly-generic helper methods.
     *
     * @return The class literal for the identity type of this model instance.
     *
     * If this is not set, we will create collections of type Model.
     */
    Class<? extends T> getModelType();
    void setModelType(Class<? extends T> modelType);

    @ComponentType("getModelType()")
    ObjectTo<ModelKey, T> getModels();

    default ObjectTo<ModelKey, T> models() {
        return getOrCreateMap(ModelKey.class, getModelType(), this::getModels, this::setModels);
    }
    void setModels(ObjectTo<ModelKey, T> models);

    default void loadModel(ModelKey key, In2<T, Throwable> in) {
        loadModel(key, in, true);
    }
    default void loadModel(ModelKey key, In2<T, Throwable> in, boolean allowNull) {
        final T val = models().get(key);
        if (val != null) {
            // TODO: optional always async?
            in.in(val, null);
            return;
        }
        X_Model.load(getModelType(), key, SuccessHandler.handler(
            success ->{
                models().put(key, success);
                in.in(success, null);
            },
            failure -> in.in(null, failure)
        ));
    }

    @Override
    @Ignore("model")
    default SetLike<T> add(T value) {
        addAndReturn(value);
        return this;
    }

    @Override
    @Ignore("model") // this method name matches regex used by model generator; forcibly exclude it
    default T addAndReturn(T value) {
        if (value == null) {
            return null;
        }
        ensureKey(value);
        final ObjectTo<ModelKey, T> mods = models();
        final ObjectTo<ModelKey, T> copy = copyMap(mods);
        final T was = models().put(value.getKey(), value);
        if (was != value) {
            fireChangeEvent("models", copy, mods);
        }
        return was;
    }

    default void ensureKey(T value) {
        X_Model.ensureKey(value.getType(), value);
    }

    @Override
    @Ignore("model") // this method name matches regex used by model generator; forcibly exclude it
    default boolean remove(T value) {
        return removeAndReturn(value) != null;
    }

    @Override
    @Ignore("model") // this method name matches regex used by model generator; forcibly exclude it
    default T removeAndReturn(T value) {
        if (value == null) {
            return null;
        }
        ensureKey(value);
        final ObjectTo<ModelKey, T> mods = models();
        final ObjectTo<ModelKey, T> copy = copyMap(mods);
        final T was = mods.remove(value.getKey());
        if (was != null) {
            fireChangeEvent("models", copy, mods);
        }
        return was;
    }

    @Override
    @Ignore("model")
    default SizedIterator<T> iterator() {
        final ObjectTo<ModelKey, T> models  = models();
        final SizedIterable<T> values = models.forEachValue();
        return values.iterator();
    }

    @Override
    @Ignore("model")
    default void clear() {
        final ObjectTo<ModelKey, T> mods = models();
        final ObjectTo<ModelKey, T> copy = copyMap(mods);
        mods.clear();
        fireChangeEvent("models", copy, mods);
    }

    @Override
    @Ignore("model")
    default int size() {
        return models().size();
    }

}
