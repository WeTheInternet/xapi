package xapi.gwt.collect;

import xapi.annotation.inject.SingletonOverride;
import xapi.collect.api.ClassTo;
import xapi.collect.api.CollectionOptions;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.collect.api.ObjectTo.Many;
import xapi.collect.api.StringDictionary;
import xapi.collect.api.StringTo;
import xapi.collect.fifo.Fifo;
import xapi.collect.impl.ClassToManyList;
import xapi.collect.impl.IntToSet;
import xapi.collect.impl.ObjectToManyList;
import xapi.collect.impl.StringToAbstract;
import xapi.collect.impl.StringToManyList;
import xapi.collect.proxy.api.CollectionProxy;
import xapi.collect.proxy.impl.MapOf;
import xapi.collect.service.CollectionService;
import xapi.fu.X_Fu;
import xapi.platform.GwtPlatform;

import java.util.Map;
import java.util.TreeMap;

@GwtPlatform
@SingletonOverride(implFor=CollectionService.class)
public class CollectionServiceGwt implements CollectionService{

  @Override
  public <Type, Generic extends Type> IntTo<Type> newList(Class<Generic> cls, CollectionOptions opts) {
    return IntToListGwt.create(()->(Type[]) X_Fu.newArray(cls, 0));
  }

  @Override
  public <E, Generic extends E> IntTo<E> newSet(
      Class<Generic> cls, CollectionOptions opts
  ) {
    return new IntToSet<>(cls, opts);
  }

  @Override
  public <K,V, Key extends K, Value extends V> ObjectTo<K,V> newMap(Class<Key> key, Class<Value> cls, CollectionOptions opts) {
    return new MapOf<>(newMap(opts), key, cls);
  }

  private <K, V> Map<K, V> newMap(CollectionOptions opts) {
    if (opts.insertionOrdered()) {
      return new java.util.LinkedHashMap<>();
    } else {
      if (opts.keyOrdered()) {
        return new java.util.TreeMap<>();
      }
      return new java.util.HashMap<>();
    }
    // No need for concurrent map types in Gwt
  }

  @Override
  public <V, Generic extends V> ClassTo<V> newClassMap(Class<Generic> cls, CollectionOptions opts) {
    return new xapi.collect.impl.ClassToDefault<V>(newMap(opts), cls);
  }

  @Override
  public <V, Generic extends V> StringTo<V> newStringMap(Class<Generic> cls, CollectionOptions opts) {
    // we ignore options because js maps are, for now, all the same
    // all maps are insertion ordered an mutable;
    // there's nothing we can do to stop you from changing a jso value ;)
    if (opts.keyOrdered()) {
      return new StringToAbstract<>(cls, new TreeMap<>());
    }
    return StringToGwt.create(cls);
  }

  @Override
  public <V> StringDictionary<V> newDictionary(Class<V> cls) {
    return JsStringDictionary.create(cls);
  }

  @Override
  public <V> StringDictionary<V> newDictionaryInsertionOrdered(Class<V> cls) {
    return JsStringDictionary.create(cls);
  }

  @Override
  public <K,V> Many<K,V> newMultiMap(final Class<K> key, final Class<V> cls, final CollectionOptions opts) {
    return new ObjectToManyList<>(key, cls, newMap(opts));
  }

  @Override
  public <V, Generic extends V> ClassTo.Many<V> newClassMultiMap(final Class<Generic> cls, final CollectionOptions opts) {
    return new ClassToManyList<>(cls, newMap(opts));
  }

  @Override
  public <V, Generic extends V> xapi.collect.api.StringTo.Many<V> newStringMultiMap(Class<Generic> cls,
    CollectionOptions opts) {
    return new StringToManyList<>(cls, newMap(opts));
  }

  @Override
  public <V> Fifo<V> newFifo() {
    return JsFifo.newFifo();
  }

  @Override
  public <K, V, Key extends K, Value extends V> CollectionProxy<K, V> newProxy(
      Class<Key> keyType, Class<Value> valueType, CollectionOptions opts
  ) {
    if (opts.insertionOrdered()) {
        return new MapOf<>(new java.util.LinkedHashMap<>(), keyType, valueType);
    }
    return new MapOf<>(new java.util.HashMap<>(), keyType, valueType);
  }

}
