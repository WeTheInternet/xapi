package xapi.fu.data;

import xapi.fu.itr.SizedIterator;

/**
 * An immutable facade over a {@link CollectionLike}:
 * - delegates read methods
 * - throws on write methods
 * - iterator().remove() is disallowed
 */
public class ImmutableCollectionLike<V> implements CollectionLike<V> {

    private final CollectionLike<V> values;

    public ImmutableCollectionLike(final CollectionLike<V> values) {
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
    public CollectionLike<V> add(final V value) {
        throw new UnsupportedOperationException("Cannot modify immutable collection");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Cannot modify immutable collection");
    }

}
