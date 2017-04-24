package xapi.collect.api;

import xapi.fu.Do;
import xapi.fu.In1Out1;
import xapi.fu.MapLike;
import xapi.fu.Out2;
import xapi.fu.iterate.SizedIterable;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/3/16.
 */
public interface HasMany <K, V> extends HasValues<K, IntTo<V>>, MapLike<K, IntTo<V>> {

  IntTo<V> newList();

  default HasMany <K, V> add(K key, V value) {
    get(key).add(value);
    return this;
  }

  @Override
  default SizedIterable<Out2<K, IntTo<V>>> forEachItem() {
    return MapLike.super.forEachItem();
  }

  default HasMany <K, V> addMany(K key, Iterable<V> value) {
    get(key).addAll(value);
    return this;
  }

  default HasMany <K, V> addMapped(V v, In1Out1<V, K> mapper) {
    K  key = mapper.io(v);
    get(key).add(v);
    return this;
  }

  default HasMany <K, V> addManyMapped(Iterable<V> values, In1Out1<V, K> mapper) {
    // for each v in value,
    Do.forEachMapped1(values,
        // take the IntTo<V> result of this.get(mapper.io(v))
        mapper.mapOut(this::get),
        // and apply IntTo::add
        IntTo::add
    );
    return this;
  }

  default IntTo<V> flatten() {
    final IntTo<V> flat = newList();
    values().forEach(flat::addAll);
    return flat;
  }

  default Iterable<Out2<K, Iterable<V>>> iterableOut() {
    return mappedOut()
        .map(out->out.mapped2(IntTo::forEach));
  }

}
