package xapi.server.vertx;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.fu.In1Out1;
import xapi.fu.In2Out1;
import xapi.fu.Lazy;
import xapi.fu.MapLike;
import xapi.fu.Out1;
import xapi.fu.Pointer;
import xapi.fu.iterate.SizedIterable;
import xapi.util.X_String;
import xapi.util.api.Destroyable;
import xapi.util.api.RequestLike;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/3/16.
 */
public class VertxRequest implements Destroyable, RequestLike {

    private final In2Out1<String, SizedIterable<String>, IntTo<String>> LIST_MAPPER = (s, i)->{
        if (i instanceof IntTo) {
            return (IntTo<String>)i;
        } else {
            final IntTo<String> copy = X_Collect.newList(String.class);
            if (i != null) {
                copy.addAll(i);
            }
            return copy;
        }
    };

    private HttpServerRequest httpRequest;
    private final Lazy<String> body;
    private final Lazy<MultimapAdapter> params;
    private final Lazy<MultimapAdapter> headers;
    private final Lazy<MapLike<String, String>> cookies;
    private boolean autoclose;

    public VertxRequest(HttpServerRequest req) {
        httpRequest = req;
        body = Lazy.deferred1(()->{
            Pointer<byte[]> body = Pointer.pointer();
            getHttpRequest().bodyHandler(buffer->
                body.in(buffer.getBytes())
            );
            return new String(body.out1());
        });
        params = Lazy.deferred1(()->new MultimapAdapter(httpRequest.params()));
        headers = Lazy.deferred1(()->new MultimapAdapter(httpRequest.headers()));
        cookies = Lazy.deferred1(readCookies());
        autoclose = true;
    }

    private Out1<MapLike<String, String>> readCookies() {
        // TODO put this in an abstract shared class
        return ()->{
            final StringTo<String> c = X_Collect.newStringMap(String.class);
            SizedIterable<String> cookies = getHeaders("Cookie");
            if (cookies.isEmpty()) {
                return c;
            }
            for (String cookie : cookies) {
                String[] bits = cookie.split(";");
                for (String bit : bits) {
                    String[] nameValue = bit.trim().split("=");
                    c.put(nameValue[0], nameValue.length == 1 ? "" : X_String.decodeURIComponent(nameValue[1]));
                }
            }

            return c;
        };
    }

    public HttpServerRequest getHttpRequest() {
        return httpRequest;
    }

    public void setHttpRequest(HttpServerRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    public static VertxRequest getOrMake(VertxRequest req, HttpServerRequest httpReq) {
        if (req == null) {
            return new VertxRequest(httpReq);
        } else {
            req.httpRequest = httpReq;
            return req;
        }
    }

    @Override
    public void destroy() {
        httpRequest = null;
    }

    @Override
    public String getPath() {
        return httpRequest.path();
    }

    public String getBody() {
        return body.out1();
    }

    public MapLike<String, SizedIterable<String>> getParams() {
        return params.out1().mapValue(LIST_MAPPER, In1Out1.downcast());
    }

    @Override
    public String getHeader(String name, Out1<String> dflt) {
        final IntTo<String> val = headers.out1().get(name);
        if (val == null || val.isEmpty()) {
            return dflt == null ? null : dflt.out1();
        }
        return val.get(0);
    }

    public MapLike<String, SizedIterable<String>> getHeaders() {
        return headers.out1().mapValue(LIST_MAPPER, In1Out1.downcast());
    }

    public final HttpServerRequest getRequest() {
        return getHttpRequest();
    }

    public final HttpServerResponse getResponse() {
        return getHttpRequest().response();
    }

    public boolean isAutoclose() {
        return autoclose;
    }

    public void setAutoclose(boolean autoclose) {
        this.autoclose = autoclose;
    }

    @Override
    public MapLike<String, String> getCookies() {
        return cookies.out1();
    }
}
