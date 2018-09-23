package xapi.server.vertx;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.fu.*;
import xapi.fu.data.MapLike;
import xapi.fu.itr.SizedIterable;
import xapi.fu.lazy.ResettableLazy;
import xapi.scope.spi.RequestLike;
import xapi.util.X_String;
import xapi.util.api.Destroyable;

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
    private final ResettableLazy<String> body;
    private final ResettableLazy<MultimapAdapter> params;
    private final ResettableLazy<MultimapAdapter> headers;
    private final ResettableLazy<MapLike<String, String>> cookies;
    private final Do resetAll;
    private boolean autoclose;

    public VertxRequest(HttpServerRequest req) {
        httpRequest = req;
        body = new ResettableLazy<>(()->{
            Pointer<byte[]> body = Pointer.pointer();
            getHttpRequest().bodyHandler(buffer->
                body.in(buffer.getBytes())
            );
            return new String(body.out1());
        });
        params =  new ResettableLazy<>(()->new MultimapAdapter(httpRequest.params()));
        headers = new ResettableLazy<>(()->new MultimapAdapter(httpRequest.headers()));
        cookies = new ResettableLazy<>(readCookies());
        autoclose = true;
        resetAll = ()->{
            body.reset();
            params.reset();
            headers.reset();
            cookies.reset();
        };
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
        return params.out1().mapValue(LIST_MAPPER, In1Out1.weaken1());
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
        return headers.out1().mapValue(LIST_MAPPER, In1Out1.weaken1());
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

    @Override
    public void reset() {
        resetAll.done();
    }

    @Override
    public String toString() {
        return "VertxRequest{" +
            "uri=" + httpRequest.uri() +
            ", method=" + httpRequest.method() +
            ", body=" + body +
            ", params=" + params +
            ", headers=" + headers +
            ", cookies=" + cookies +
            ", autoclose=" + autoclose +
            '}';
    }
}
