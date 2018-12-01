package xapi.fu.data;

import xapi.fu.Do;
import xapi.fu.In1Out1;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/25/18 @ 8:09 AM.
 */
public interface IsMulti <K, V, C extends CollectionLike<V>> extends MapLike<K, C> {

    default IsMulti<K, V, C> addMany(K key, Iterable<V> value) {
        get(key).addNow(value);
        return this;
    }

    default IsMulti <K, V, C> addMapped(V v, In1Out1<V, K> mapper) {
        K  key = mapper.io(v);
        get(key).add(v);
        return this;
    }

    default IsMulti<K, V, C> addManyMapped(Iterable<V> values, In1Out1<V, K> mapper) {
        // for each v in value,
        Do.forEachMapped1(values,
            // take the IntTo<V> result of this.get(mapper.io(v))
            mapper.mapOut(this::get),
            // and apply IntTo::add
            CollectionLike::add
        );
        return this;
    }
}
