package xapi.gwt.collect;

import xapi.annotation.inject.SingletonOverride;
import xapi.collect.api.ClassTo;
import xapi.collect.api.CollectionOptions;
import xapi.collect.api.Fifo;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.collect.api.ObjectTo.Many;
import xapi.collect.api.StringDictionary;
import xapi.collect.api.StringTo;
import xapi.collect.service.CollectionService;
import xapi.except.NotYetImplemented;
import xapi.platform.GwtPlatform;

@GwtPlatform
@SingletonOverride(implFor=CollectionService.class)
public class CollectionServiceGwt implements CollectionService{

  @Override
  public <V> IntTo<V> newList(Class<V> cls, CollectionOptions opts) {
    return IntToListGwt.newInstance();
  }

  @Override
  public <V> IntTo<V> newSet(Class<V> cls, CollectionOptions opts) {
    throw new NotYetImplemented("IntTo not yet implemented");
  }

  @Override
  public <K,V> ObjectTo<K,V> newMap(Class<K> key, Class<V> cls, CollectionOptions opts) {
    return new xapi.collect.proxy.MapOf<K, V>(new java.util.HashMap<K, V>(), key, cls);
  }

  @Override
  public <V> ClassTo<V> newClassMap(Class<V> cls, CollectionOptions opts) {
    return new xapi.collect.impl.ClassToDefault<V>(new java.util.HashMap<Class<?>, V>(), cls);
  }

  @Override
  public <V> StringTo<V> newStringMap(Class<V> cls, CollectionOptions opts) {
    // we ignore options because js maps are, for now, all the same
    // all maps are insertion ordered an mutable;
    // there's nothing we can do to stop you from changing a jso value ;)
    return StringToGwt.create(cls);
  }

  @Override
  public <V> StringDictionary<V> newDictionary() {
    return JsStringDictionary.create();
  }

  @Override
  public <K,V> Many<K,V> newMultiMap(Class<K> key, Class<V> cls, CollectionOptions opts) {
    throw new NotYetImplemented("MultiMap not yet implemented");
  }

  @Override
  public <V> xapi.collect.api.ClassTo.Many<V> newClassMultiMap(Class<V> cls, CollectionOptions opts) {
    throw new NotYetImplemented("MultiMap not yet implemented");
  }

  @Override
  public <V> xapi.collect.api.StringTo.Many<V> newStringMultiMap(Class<V> cls,
    CollectionOptions opts) {
    throw new NotYetImplemented("MultiMap not yet implemented");
  }

  @Override
  public <V> Fifo<V> newFifo() {
    return JsFifo.newFifo();
  }

}
