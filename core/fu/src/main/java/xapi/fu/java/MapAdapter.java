package xapi.fu.java;

import xapi.fu.data.MapLike;
import xapi.fu.iterate.SizedIterable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple {@link MapLike} delegate over a java map.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/21/17.
 */
public class MapAdapter <K, V> implements MapLike<K, V>, Serializable {

    private final Map<K, V> map;

    public MapAdapter() {
        this(new HashMap<>());
    }

    public MapAdapter(Map<K, V> map) {
        this.map = map;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public V put(K key, V value) {
        return map.put(key, value);
    }

    @Override
    public V get(K key) {
        return map.get(key);
    }

    @Override
    public boolean has(K key) {
        return map.containsKey(key);
    }

    @Override
    public V remove(K key) {
        return map.remove(key);
    }

    @Override
    public SizedIterable<K> keys() {
        return SizedIterable.of(this::size, map.keySet());
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public String toString() {
        return map.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final MapAdapter<?, ?> that = (MapAdapter<?, ?>) o;

        return map.equals(that.map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }
}
