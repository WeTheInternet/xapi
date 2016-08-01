package xapi.util.api;

import xapi.fu.Lazy;

import static xapi.fu.Out1.out1Deferred;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 7/29/16.
 */
public interface IdGenerator<T> {
    String generateId(T from);

    default Lazy<String> lazyId(T from) {
        return Lazy.deferred1(out1Deferred(this::generateId, from));
    }
}
