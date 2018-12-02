package xapi.fu.has;

import xapi.fu.api.Ignore;
import xapi.fu.itr.MappedIterable;
import xapi.fu.Mutable;
import xapi.fu.X_Fu;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/8/16.
 */
@Ignore("model")
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
