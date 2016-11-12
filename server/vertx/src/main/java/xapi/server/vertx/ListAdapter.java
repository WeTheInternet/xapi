package xapi.server.vertx;

import xapi.collect.api.CollectionOptions;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.fu.In2Out1;
import xapi.fu.X_Fu;
import xapi.util.X_Util;
import xapi.util.impl.AbstractPair;

import java.util.*;
import java.util.Map.Entry;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/8/16.
 */
public class ListAdapter<T> implements IntTo<T> {

    private final List<T> list;

    public ListAdapter(List<T> list) {
        this.list = list == null ? Collections.EMPTY_LIST : list;
    }

    @Override
    public Iterable<T> forEach() {
        return list;
    }

    @Override
    public boolean add(T item) {
        return list.add(item);
    }

    @Override
    public boolean addAll(Iterable<T> items) {
        if (items instanceof Collection) {
            return list.addAll((Collection)items);
        }
        items.forEach(list::add);
        return true;
    }

    @Override
    public boolean addAll(T... items) {
        for (T item : items) {
            list.add(item);
        }
        return true;
    }

    @Override
    public boolean insert(int pos, T item) {
        list.add(pos, item);
        return true;
    }

    @Override
    public boolean contains(T value) {
        return list.contains(value);
    }

    @Override
    public T at(int index) {
        return list.get(index);
    }

    @Override
    public int indexOf(T value) {
        return list.indexOf(value);
    }

    @Override
    public boolean remove(int index) {
        return list.remove(index) != null;
    }

    @Override
    public boolean findRemove(T value, boolean all) {
        if (all) {
            boolean found = false;
            for (int i = list.size(); i --> 0; ) {
                if (X_Util.equal(value, list.get(i))) {
                    list.remove(i);
                    found = true;
                }
            }
            return found;
        } else {
            for (int i = 0, m = list.size(); i < m; i++ ) {
                if (X_Util.equal(value, list.get(i))) {
                    list.remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void set(int index, T value) {
        list.set(index, value);
    }

    @Override
    public void push(T value) {
        list.add(value);
    }

    @Override
    public T pop() {
        return list.remove(list.size()-1);
    }

    @Override
    public List<T> asList() {
        return new ArrayList<>(list);
    }

    @Override
    public Set<T> asSet() {
        return new LinkedHashSet<>(list);
    }

    @Override
    public Deque<T> asDeque() {
        return new LinkedList<T>(list);
    }

    @Override
    public ObjectTo<Integer, T> clone(CollectionOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T put(Entry<Integer, T> item) {
        return list.set(item.getKey(), item.getValue());
    }

    @Override
    public Entry<Integer, T> entryFor(Object key) {
        int index = list.indexOf(key);
        return new AbstractPair<>(index, index == -1 ? null : (T)key);
    }

    @Override
    public T get(Object key) {
        return at((Integer)key);
    }

    @Override
    public void setValue(Object key, Object value) {
        set((Integer)key, (T)value);
    }

    @Override
    public T remove(Object key) {
        return list.remove(((Integer)key).intValue());
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public T[] toArray() {
        return list.toArray(X_Fu.array());
    }

    @Override
    public Collection<T> toCollection(Collection<T> into) {
        if (into == null) {
            return asList();
        }
        into.clear();
        into.addAll(list);
        return into;
    }

    @Override
    public Map<Integer, T> toMap(Map<Integer, T> into) {
        if (into == null) {
            into = new LinkedHashMap<>();
        } else {
            into.clear();
        }
        int i = 0;
        for (T t : list) {
            into.put(i++, t);
        }
        return into;
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public Class<Integer> keyType() {
        return Integer.class;
    }

    @Override
    public Class<T> valueType() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean readWhileTrue(In2Out1<Integer, T, Boolean> callback) {
        int i = 0;
        for (T t : list) {
            if (!callback.io(i++, t)) {
                return false;
            }
        }
        return true;
    }
}
