package xapi.scope.spi;

import xapi.fu.data.MapLike;
import xapi.fu.Out1;
import xapi.fu.iterate.EmptyIterator;
import xapi.fu.iterate.SizedIterable;

/**
 * A representation of a http-like request object;
 * you should mostly just satisfy this interface with internal types,
 * where you will probably use the "native" forms of these methods
 * (i.e. call methods on your own req/resp container objects).
 *
 *
 * Created by James X. Nelson (james @wetheinter.net) on 11/26/16.
 */
public interface RequestLike {

    String getPath();

    String getBody();

    default String getHeader(String name, Out1<String> dflt) {
        final SizedIterable<String> headers = getHeaders(name);
        if (headers.isEmpty()) {
            return dflt == null ? null : dflt.out1();
        }
        return headers.first();
    }

    default String getParam(String name, Out1<String> dflt) {
        final SizedIterable<String> params = getParams(name);
        if (params.isEmpty()) {
            return dflt == null ? null : dflt.out1();
        }
        return params.first();
    }

    default SizedIterable<String> getHeaders(String name) {
        final MapLike<String, SizedIterable<String>> headers = getHeaders();
        return headers.getOrSupply(name, EmptyIterator::none);
    }

    default SizedIterable<String> getParams(String name) {
        final MapLike<String, SizedIterable<String>> params = getParams();
        return params.getOrSupply(name, EmptyIterator::none);
    }

    MapLike<String, SizedIterable<String>> getParams();

    MapLike<String, SizedIterable<String>> getHeaders();

    MapLike<String, String> getCookies();

    void reset();
}
