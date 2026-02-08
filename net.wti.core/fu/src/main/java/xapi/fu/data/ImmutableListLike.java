package xapi.fu.data;

import xapi.fu.itr.SizedIterator;

/**
 * An immutable facade over a {@link ListLike}:
 * - delegates read methods
 * - throws on write methods
 * - iterator().remove() is disallowed
 */
public class ImmutableListLike<V> implements ListLike<V> {

    private final ListLike<V> values;

    public ImmutableListLike(final ListLike<V> values) {
        this.values = values;
    }

    @Override
    public V get(final int pos) {
        return values.get(pos);
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
    public V set(final int pos, final V value) {
        throw new UnsupportedOperationException("Cannot modify immutable list");
    }

    @Override
    public V remove(final int pos) {
        throw new UnsupportedOperationException("Cannot modify immutable list");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Cannot modify immutable list");
    }

}
