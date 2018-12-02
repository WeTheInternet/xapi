package xapi.fu.has;

import xapi.fu.Do;
import xapi.fu.In1;

/**
 * The "red-blue inverse" of {@link HasItems}.
 *
 * HasItems is used to return a MappedIterable of some items,
 * and is synchronous (a "blue" method).
 *
 * HasData is used to pass an {@link xapi.fu.In1} callback,
 * which is invoked with 0+ items at some time in the future (a "red" method).
 *
 * Created by James X. Nelson (James@WeTheInter.net) on 9/16/18 @ 4:51 AM.
 */
public interface HasData <V> {

    /**
     * Subscribe to all values within this collection.
     *
     * Note that latency is NOT guaranteed or enforced in any way;
     * your callback could be invoked N times immediately,
     * then 0 to M times later.
     *
     * @param listener The callback to invoke with items inside this collection
     * @return A callback to remove the subscription at a later time.
     */
    Do subscribe(In1<V> listener);

    /**
     * Read all the current items in the collection,
     * but do not invoke the callback on items added after this method completes.
     *
     * No guarantee is made about items added while the snapshot is being read.
     *
     * @param listener The callback to invoke with each item inside the collection.
     */
    default void snapshot(In1<V> listener) {
        subscribe(listener).done();
    }

}
