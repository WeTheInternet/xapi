package xapi.fu.data;

import xapi.fu.In1;
import xapi.fu.In2;
import xapi.fu.In2Out1;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/25/18 @ 7:08 AM.
 */
public interface MultiList <K, V> extends
    MapLike<K, ListLike<V>>,
    IsMulti<K, V, ListLike<V>> {

    ListLike<V> createList(K key);

    default ListLike<V> flatten() {
        final ListLike<V> into = createList(null);
        mappedValues().forAll(into::addNow);
        return into;
    }

    default boolean addItem(K key, V value) {
        get(key).add(value);
        return true;
    }

    default void forEachPair(In2<K, V> consumer) {
        forAll(item-> {
            final In1<V> reduced = consumer.provide1(item.out1());
            item.out2().forAll(reduced);
        });
    }

}
