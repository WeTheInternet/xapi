package xapi.fu.data;

/**
 * Created by James X. Nelson (James@WeTheInter.net) on 11/25/18 @ 7:08 AM.
 */
public interface MultiSet <K, V> extends
    MapLike<K, SetLike<V>>,
    IsMulti<K, V, SetLike<V>> {

    SetLike<V> createSet(K key);

    default SetLike<V> flatten() {
        final SetLike<V> into = createSet(null);
        mappedValues().forAll(into::addNow);
        return into;
    }
}
