package xapi.model.api;

import xapi.collect.X_Collect;
import xapi.collect.api.CollectionOptions;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.collect.api.StringTo;
import xapi.collect.proxy.api.CollectionProxy;
import xapi.fu.*;
import xapi.fu.api.DoNotOverride;
import xapi.fu.has.HasLock;
import xapi.fu.itr.MappedIterable;
import xapi.model.X_Model;

import java.util.Map.Entry;

import static xapi.collect.X_Collect.MUTABLE_CONCURRENT_INSERTION_ORDERED;

public interface Model {

    //attributes
    <T> T getProperty(String key);

    <T> T getProperty(String key, T dflt);

    <T> T getProperty(String key, Out1<T> dflt);

    default <T> T getOrSaveProperty(String key, Out1<T> dflt) {
        return HasLock.alwaysLock(this, ()->{
            // only set a property if this key is not already defined;
            // this prevents us from recording the output of dflt.out1(), so you can detect "value not set"
            boolean save = !hasProperty(key);
            T val = getProperty(key, dflt);
            if (save) {
                setProperty(key, val);
            }
            return val;
        });
    }
    default <T, I1> T getOrSaveProperty(String key, In1Out1<I1, T> dflt, I1 arg) {
        return getOrSaveProperty(key, dflt.supply(arg));
    }

    default <T> T getOrCreate(Out1<T> getter, Out1<T> factory, In1<T> setter) {
        return HasLock.alwaysLock(this, ()->{
            T is = getter.out1();
            if (is == null) {
                is = factory.out1();
                setter.in(is);
            }
            return is;
        });
    }

    default <T, G extends T> StringTo<T> getOrCreateStringMap(
        Class<G> type,
        Out1<StringTo<T>> getter,
        In1<StringTo<T>> setter
    ) {
        return getOrCreate(getter, () -> X_Collect.newStringMap(type), setter);
    }
    default <K, Key extends K, V, Val extends V> ObjectTo<K, V> getOrCreateMap(
        Class<Key> keyType,
        Class<Val> type,
        Out1<ObjectTo<K, V>> getter,
        In1<ObjectTo<K, V>> setter
    ) {
        return getOrCreateMap(keyType, type, getter, setter, null);
    }
    default <K, Key extends K, V, Val extends V> ObjectTo<K, V> getOrCreateMap(
        Class<Key> keyType,
        Class<Val> type,
        Out1<ObjectTo<K, V>> getter,
        In1<ObjectTo<K, V>> setter,
        CollectionOptions opts
    ) {
        return getOrCreate(getter, () ->
            X_Collect.newMap(keyType, type, opts == null ? MUTABLE_CONCURRENT_INSERTION_ORDERED : opts)
            , setter);
    }

    default <T, G extends T> IntTo<T> getOrCreateList(Class<G> type, Out1<IntTo<T>> getter, In1<IntTo<T>> setter) {
        return getOrCreate(getter, () -> X_Collect.newList(type), setter);
    }

    default <T extends Model, G extends T> ModelList<T> getOrCreateModelList(Class<G> type, Out1<ModelList<T>> getter, In1<ModelList<T>> setter) {
        return getOrCreate(getter, ()->{
            final ModelList list = X_Model.create(ModelList.class);
            list.setModelType(type);
            final String name = type.getSimpleName();
            final ModelKey myKey = getKey();
            if (myKey != null) {
                list.setKey(myKey.getChild(ModelList.MODEL_LIST, name));
            }
            return list;
        }, setter);
    }

    default <T> T compute(Out1<T> getter, In1Out1<T, T> mapper, In1<T> setter) {
        return HasLock.alwaysLock(this, ()->{
            T was = getter.out1();
            final T is = mapper.io(was);
            if (was != is) {
                setter.in(is);
            }
            return is;
        });
    }

    default <T> Maybe<T> getMaybe(String key) {
        final T val = getProperty(key);
        return Maybe.nullable(val);
    }

    boolean hasProperty(String key);

    Class<?> getPropertyType(String key);

    MappedIterable<Entry<String, Object>> getProperties();

    String[] getPropertyNames();

    Model setProperty(String key, Object value);

    /**
     * @param key - The property name to register the callback for.
     * @param callback - A function which receives oldValue,newValue as arguments.
     */
    void onChange(String key, In2<Object, Object> callback);

    /**
     * Triggers a change event. Only invoke this when was != value
     * @param key - The model key for which the change applies
     * @param was - The previous value for this key
     * @param value - The current value for this key
     */
    void fireChangeEvent(String key, Object was, Object value);

    /**
     * @param callback - A function which receives propKey,oldValue,newValue as arguments.
     */
    void onGlobalChange(In3<String, Object, Object> callback);

    default void globalChange(In3<String, Object, Object> callback) {
        onGlobalChange(callback);
        for (Entry<String, Object> prop : getProperties()) {
            if (prop.getValue() != null) {
                callback.in(prop.getKey(), null, prop.getValue());
            }
        }

    }

    Model removeProperty(String key);

    void clear();

    ModelKey getKey();

    default ModelKey key() {
        ModelKey key = getKey();
        if (key == null) {
            key = X_Model.newKey(getType());
            setKey(key);
        }
        return key;
    }

    default boolean hasKey() {
        return getKey() != null;
    }

    default boolean hasKeyFilled() {
        final ModelKey key = getKey();
        return key != null && key.isComplete();
    }

    default ModelKey getOrComputeKey(Out1<ModelKey> backup) {
        ModelKey key = getKey();
        if (key == null) {
            key = backup.out1();
            setKey(key);
        }
        return key;
    }

    void setKey(ModelKey key);

    String getType();

    default void absorb(Model model) {
        absorb(model, false);
    }

    default void absorb(Model model, boolean append) {
        ModelCopier.copy(model, this, append);
    }

    default boolean isKeyResolved() {
        final ModelKey key = getKey();
        if (key == null) {
            return false;
        }
        return key.isComplete();
    }

    @DoNotOverride
    default boolean isKeyUnresolved() {
        return !isKeyResolved();
    }
}
