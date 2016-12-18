package xapi.fu.iterate;

import java.util.Iterator;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/9/16.
 */
public interface GrowableIterable<T> extends Iterable<T> {

    @Override
    GrowableIterator<T> iterator();

    static <T> GrowableIterable<T> growable(T one) {
        return new GrowableIterator<>(one).forAll();
    }

    static <T> GrowableIterable<T> growable(Iterable<T> one) {
        return GrowableIterator.of(one).forAll();
    }

    static <T> GrowableIterable<T> growable(Iterator<T> one) {
        return GrowableIterator.of(one).forAll();
    }

    default GrowableIterable<T> concat(Iterable<T> others) {
        return new GrowableIterator<>(this).concat(others).forAll();
    }

    default GrowableIterable<T> add(T value) {
        return new GrowableIterator<>(this).concat(value).forAll();
    }


    static <T> GrowableIterable<T> of(Iterable<T> one) {
        if (one instanceof GrowableIterable) {
            return (GrowableIterable<T>) one;
        }
        GrowableIterator<T> itr = new GrowableIterator<>(one);
        return new GrowableIterable<T>() {
            @Override
            public GrowableIterator<T> iterator() {
                return itr;
            }
        };
    }

    static <T> GrowableIterable<T> of(Iterator<T> one) {
        if (one instanceof GrowableIterator) {
            return new GrowableIterable<T>() {
                @Override
                public GrowableIterator<T> iterator() {
                    return (GrowableIterator<T>) one;
                }
            };
        }
        final GrowableIterator<T> itr = new GrowableIterator<>(one);
        return new GrowableIterable<T>() {
            @Override
            public GrowableIterator<T> iterator() {
                return itr;
            }
        };
    }

    static <T> GrowableIterable<T> of(T one) {
        return of(SingletonIterator.singleItem(one));
    }

}
