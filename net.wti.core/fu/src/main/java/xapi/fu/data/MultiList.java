package xapi.fu.data;

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

}
