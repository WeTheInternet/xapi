package xapi.fu.data;

import xapi.fu.itr.SizedIterator;

/**
 * An immutable facade over a {@link SetLike}:
 * - delegates read methods
 * - throws on write methods
 * - iterator().remove() is disallowed
 */
public class ImmutableSetLike<V> implements SetLike<V> {

    private final SetLike<V> values;

    public ImmutableSetLike(final SetLike<V> values) {
        this.values = values;
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public SizedIterator<V> iterator() {
        return values.iterator().readOnly();
    }

    @Override
    public V addAndReturn(final V value) {
        throw new UnsupportedOperationException("Cannot modify immutable set");
    }

    @Override
    public V removeAndReturn(final V value) {
        throw new UnsupportedOperationException("Cannot modify immutable set");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Cannot modify immutable set");
    }

}
