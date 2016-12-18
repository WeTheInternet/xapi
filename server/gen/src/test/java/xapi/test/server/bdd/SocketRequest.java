package xapi.test.server.bdd;

import xapi.fu.Out2;
import xapi.util.api.RequestLike;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/26/16.
 */
public class SocketRequest implements RequestLike {
    @Override
    public String getPath() {
        return null;
    }

    @Override
    public String getBody() {
        return null;
    }

    @Override
    public Iterable<Out2<String, Iterable<String>>> getParams() {
        return null;
    }

    @Override
    public Iterable<Out2<String, Iterable<String>>> getHeaders() {
        return null;
    }
}
