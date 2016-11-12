package xapi.fu;

import java.util.Iterator;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/31/16.
 */
public class MappedIterator<From, To> implements Iterator<To> {

    private final Iterator<From> from;
    private final In1Out1<? super From, ? extends To> mapper;

    public MappedIterator(Iterator<From> from, In1Out1<? super From, ? extends To> mapper) {
        this.from = from;
        this.mapper = mapper;
    }

    public static <From, To> MappedIterator<From, To> mapIterator(Iterator<From> from, In1Out1<From, To> mapper) {
        return new MappedIterator<>(from, mapper);
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
}
