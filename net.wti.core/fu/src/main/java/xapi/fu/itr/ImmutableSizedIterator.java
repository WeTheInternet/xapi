package xapi.fu.itr;

import xapi.fu.IsImmutable;

///
/// ImmutableSizedIterator:
///
/// An immutable facade over a sized iterator
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 02:25
public class ImmutableSizedIterator <T> implements IsImmutable, SizedIterator<T> {

    public static <T> SizedIterator<T> of(final SizedIterator<T> itr) {
        if (itr instanceof IsImmutable) {
            return itr;
        }
        return itr.readOnly();
    }

    private final SizedIterator<T> itr;

    public ImmutableSizedIterator(final SizedIterator<T> itr) {
        this.itr = itr;
    }

    @Override
    public boolean hasNext() {
        return itr.hasNext();
    }

    @Override
    public T next() {
        return itr.next();
    }

    @Override
    public int size() {
        return itr.size();
    }

    @Override
    public SizedIterator<T> readOnly() {
        return this;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Cannot remove from immutable iterator");
    }
}
