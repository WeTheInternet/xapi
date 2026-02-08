package xapi.fu.data;

import xapi.fu.Out2;
import xapi.fu.itr.SizedIterable;
import xapi.fu.itr.SizedIterator;
import xapi.fu.java.X_Jdk;

///
/// ImmutableMapLike:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 21/12/2025 @ 16:06
public class ImmutableMapLike <K, V> implements MapLike<K, V> {

    private final MapLike<K, V> values;

    public ImmutableMapLike(final MapLike<K, V> values) {
        this.values = X_Jdk.mapOrderedInsertion();
        this.values.addNow(values);
    }

    @Override
    public V get(K key) {
        return values.get(key);
    }

    @Override
    public boolean has(K key) {
        return values.has(key);
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public SizedIterator<Out2<K, V>> iterator() {
        return values.iterator().readOnly();
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException("Cannot modify immutable map");
    }

    @Override
    public V remove(K key) {
        throw new UnsupportedOperationException("Cannot modify immutable map");
    }

    @Override
    public SizedIterable<K> keys() {
        return values.keys().readOnly();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Cannot modify immutable map");
    }

}
