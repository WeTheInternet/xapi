package xapi.fu.has;

import xapi.fu.MappedIterable;
import xapi.fu.Mutable;
import xapi.fu.X_Fu;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/8/16.
 */
public interface HasItems <T> {

    MappedIterable<T> forEachItem();

    default MappedIterable<T> forItems(int size) {
        final Mutable<Integer> left = new Mutable<>(size);
        return forEachItem()
            .filter(i->
                left.compute(X_Fu::minusOne) >= 0
            );
    }
}
