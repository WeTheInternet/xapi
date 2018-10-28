package xapi.fu.data;

import xapi.fu.api.Clearable;
import xapi.fu.api.Ignore;
import xapi.fu.has.HasItems;
import xapi.fu.itr.SizedIterable;

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
}
