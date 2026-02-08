package xapi.fu.itr;

import xapi.fu.IsImmutable;

///
/// ImmutableSizedIterable:
///
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 02:31
public class ImmutableSizedIterable <T> implements IsImmutable, SizedIterable<T> {

    public static <T> SizedIterable<T> of(final SizedIterable<T> itr) {
        if (itr instanceof IsImmutable) {
            return itr;
        }
        return itr.readOnly();
    }

    private final SizedIterable<T> itr;

    public ImmutableSizedIterable(SizedIterable<T> toWrap) {
        this.itr = toWrap;
    }

    @Override
    public SizedIterator<T> iterator() {
        return itr.iterator().readOnly();
    }

    @Override
    public SizedIterable<T> readOnly() {
        return this;
    }

    @Override
    public int size() {
        return itr.size();
    }
}
