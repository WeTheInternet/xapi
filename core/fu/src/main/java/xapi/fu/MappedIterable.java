package xapi.fu;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/31/16.
 */
public interface MappedIterable<T> extends Iterable<T> {

    default <To> MappedIterable<To> map(In1Out1<T, To> mapper) {
        return mapIterable(this, mapper);
    }

    static <From, To> MappedIterable<To> mapIterable(Iterable<From> from, In1Out1<From, To> mapper) {
        return ()->new MappedIterator<>(from.iterator(), mapper);
    }
}
