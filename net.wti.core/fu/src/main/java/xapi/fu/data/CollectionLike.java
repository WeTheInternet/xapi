package xapi.fu.data;

import xapi.fu.Out2;
import xapi.fu.api.Clearable;
import xapi.fu.api.Ignore;
import xapi.fu.has.HasItems;
import xapi.fu.itr.SizedIterable;
import xapi.fu.itr.SizedIterator;

import java.util.Iterator;

/**
 * Lists, Sets AND Maps all implement CollectionLike (with maps using Out2 tuples of K:V for standard iteration).
 *
 * You should strive to override as many methods as you can with more performant overrides.
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 9/16/18 @ 4:44 AM.
 */
@Ignore("model")
public interface CollectionLike <V> extends Clearable, SizedIterable<V>, HasItems<V> {

    @Override
    default SizedIterable<V> forEachItem() {
        return this;
    }

    CollectionLike<V> add(V value);

    default <N> SizedIterable<Out2<V, N>> merge(boolean shrink, CollectionLike<N> other) {
        return SizedIterable.of(
            ()-> shrink ? Math.min(size(), other.size()) : Math.max(size(), other.size()),
            ()-> {
                final SizedIterator<V> me = iterator();
                final SizedIterator<N> you = other.iterator();
                return new Iterator<Out2<V, N>>() {
                    @Override
                    public boolean hasNext() {
                        return me.hasNext() ? !shrink || you.hasNext() : !shrink && you.hasNext();
                    }

                    @Override
                    public Out2<V, N> next() {
                        return Out2.out2Immutable(
                            me.hasNext() ? me.next() : null,
                            you.hasNext() ? you.next() : null
                        );
                    }
                };
            });
    }

    default CollectionLike<V> addNow(Iterable<? extends V> items) {
        items.forEach(this::add);
        return this;
    }

}
