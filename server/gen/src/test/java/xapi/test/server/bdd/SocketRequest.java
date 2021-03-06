package xapi.test.server.bdd;

import xapi.collect.X_Collect;
import xapi.collect.api.ObjectTo;
import xapi.fu.data.MapLike;
import xapi.fu.itr.SizedIterable;
import xapi.scope.spi.RequestLike;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 11/26/16.
 */
public class SocketRequest implements RequestLike {
    private MapLike<String, SizedIterable<String>> params;
    private MapLike<String, SizedIterable<String>> headers;
    private String path;
    private String body;
    private String method;

    public SocketRequest() {
        ObjectTo<String, SizedIterable<String>> map = X_Collect.newMap(String.class, SizedIterable.class);
        params = map.asMap();
        map = X_Collect.newMap(String.class, SizedIterable.class);
        headers = map.asMap();
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getBody() {
        return body;
    }

    @Override
    public MapLike<String, SizedIterable<String>> getParams() {
        return params;
    }

    @Override
    public MapLike<String, SizedIterable<String>> getHeaders() {
        return headers;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public MapLike<String, String> getCookies() {
        return null;
    }

    @Override
    public void reset() {

    }

    @Override
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public String getEncoding() {
        return "UTF-8";
    }
}
