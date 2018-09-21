package xapi.fu.itr;

import xapi.fu.In1Out1;
import xapi.fu.Out1;

import java.util.Iterator;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/31/16.
 */
public class MappedSizedIterator<From, To> implements SizedIterator<To> {

    private final SizedIterator<From> from;
    private final In1Out1<? super From, ? extends To> mapper;

    public MappedSizedIterator(SizedIterator<From> from, In1Out1<? super From, ? extends To> mapper) {
        this.from = from;
        this.mapper = mapper;
    }

    public MappedSizedIterator(Out1<Integer> size, Iterator<From> from, In1Out1<? super From, ? extends To> mapper) {
        this(SizedIterator.of(size, from), mapper);
    }

    public static <From, To> MappedSizedIterator<From, To> mapIterator(SizedIterator<From> from, In1Out1<From, To> mapper) {
        return new MappedSizedIterator<>(from, mapper);
    }

    public static <From, To> MappedSizedIterator<From, To> mapIterator(Out1<Integer> size, Iterator<From> from, In1Out1<From, To> mapper) {
        return new MappedSizedIterator<>(size, from, mapper);
    }

    @Override
    public boolean hasNext() {
        return from.hasNext();
    }

    @Override
    public To next() {
        return mapper.io(from.next());
    }

    @Override
    public void remove() {
        from.remove();
    }

    @Override
    public int size() {
        return from.size();
    }
}
