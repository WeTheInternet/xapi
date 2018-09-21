package xapi.fu.java;

import xapi.fu.data.SetLike;
import xapi.fu.itr.SizedIterator;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/21/17.
 */
public class SetAdapter <V> implements SetLike<V> {

    private final Set<V> set;

    public SetAdapter(Set<V> set) {
        this.set = set;
    }

    public <Placeholder> SetAdapter(Map<V, Placeholder> map, Placeholder value) {
        this(new Set<V>() {

            @Override
            public int size() {
                return map.size();
            }

            @Override
            public boolean isEmpty() {
                return map.isEmpty();
            }

            @Override
            public boolean contains(Object o) {
                return map.containsKey(o);
            }

            @Override
            public Iterator<V> iterator() {
                return map.keySet().iterator();
            }

            @Override
            public Object[] toArray() {
                return map.keySet().toArray();
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return map.keySet().toArray(a);
            }

            @Override
            public boolean add(V v) {
                if (map.containsKey(v)) {
                    return false;
                }
                map.put(v, value);
                return true;
            }

            @Override
            public boolean remove(Object o) {
                if (map.containsKey(o)) {
                    map.remove(o);
                    return true;
                }
                return false;
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return map.keySet().containsAll(c);
            }

            @Override
            public boolean addAll(Collection<? extends V> c) {
                boolean changed = false;
                for (V v : c) {
                    if (add(v)) {
                        changed = true;
                    }
                }
                return changed;
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                final Iterator<V> itr = map.keySet().iterator();
                boolean changed = false;
                while (itr.hasNext()) {
                    if (!c.contains(itr.next())) {
                        itr.remove();
                        changed = true;
                    }
                }
                return changed;
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                boolean changed = false;
                for (Object o : c) {
                    if (remove(o)) {
                        changed = true;
                    }
                }
                return changed;
            }

            @Override
            public void clear() {
                map.clear();
            }
        });
    }

    @Override
    public boolean contains(V value) {
        return set.contains(value);
    }

    @Override
    public void clear() {
        set.clear();
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public SizedIterator<V> iterator() {
        return SizedIterator.of(size(), set.iterator());
    }

    @Override
    public V addAndReturn(V value) {
        if (value == null) {
            // no nulls, k thx
            return null;
        }
        if (set.add(value)) {
            return value;
        }
        return null;
    }

    @Override
    public V removeAndReturn(V value) {
        if (set.remove(value)) {
            return value;
        }
        return null;
    }

    @Override
    public String toString() {
        return set.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        final SetAdapter<?> that = (SetAdapter<?>) o;

        return set.equals(that.set);
    }

    @Override
    public int hashCode() {
        return set.hashCode();
    }
}
