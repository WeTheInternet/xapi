package xapi.server.vertx;

import io.vertx.core.MultiMap;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.fu.MappedIterable;
import xapi.fu.Out2;
import xapi.fu.iterate.SizedIterable;
import xapi.fu.iterate.SizedIterator;
import xapi.util.X_Util;
import xapi.util.impl.AbstractPair;

import java.util.List;
import java.util.Map.Entry;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/8/16.
 */
public class MultimapAdapter implements StringTo.Many<String> {

    private final MultiMap map;

    public MultimapAdapter(MultiMap map) {
        this.map = map;
    }

    @Override
    public IntTo<String> newList() {
        return null;
    }

    @Override
    public String[] keyArray() {
        return map.names().toArray(new String[map.size()]);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public SizedIterator<Out2<String, IntTo<String>>> iterator() {
        return SizedIterator.of(this::size, entries().map(Out2::fromEntry).iterator());
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.contains(String.valueOf(key));
    }

    @Override
    public boolean containsValue(Object key) {
        for (Entry<String, String> entry : map.entries()) {
            if (X_Util.equal(key, entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void putAll(Iterable<Entry<String, IntTo<String>>> items) {
        for (Entry<String, IntTo<String>> item : items) {
            map.set(item.getKey(), item.getValue().forEach());
        }
    }

    @Override
    public void addAll(Iterable<Out2<String, IntTo<String>>> items) {

    }

    @Override
    public void removeAll(Iterable<String> items) {
        items.forEach(map::remove);
    }

    @Override
    public IntTo<String> put(String key, IntTo<String> value) {
        final List<String> was = map.getAll(key);
        map.set(key, value.forEach());
        return new ListAdapter<>(was);
    }

    @Override
    public IntTo<String> get(String key) {
        return new ListAdapter<>(map.getAll(key));
    }

    @Override
    public boolean has(String key) {
        return map.contains(key);
    }

    @Override
    public IntTo<String> remove(String key) {
        final List<String> was = map.getAll(key);
        map.remove(key);
        return new ListAdapter<>(was);
    }

    @Override
    public SizedIterable<String> keys() {
        return SizedIterable.of(this::size, map.names());
    }

    @Override
    public Iterable<IntTo<String>> values() {
        return MappedIterable.mapped(map.names())
            .map(map::getAll)
            .map(ListAdapter::new);
    }

    @Override
    public Class<String> keyType() {
        return String.class;
    }

    @Override
    public Class<IntTo<String>> valueType() {
        return Class.class.cast(IntTo.class);
    }

    @Override
    public MappedIterable<Entry<String, IntTo<String>>> entries() {
        return MappedIterable.mapped(map.names())
            .map(name->new AbstractPair<String, IntTo<String>>(name, new ListAdapter<>(map.getAll(name))));
    }
}
