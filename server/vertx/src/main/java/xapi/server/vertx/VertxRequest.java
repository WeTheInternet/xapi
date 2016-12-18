package xapi.server.vertx;

import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import xapi.collect.api.StringTo;
import xapi.fu.Lazy;
import xapi.fu.Out2;
import xapi.fu.Pointer;
import xapi.util.api.Destroyable;
import xapi.util.api.RequestLike;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/3/16.
 */
public class VertxRequest implements Destroyable, RequestLike {

    private HttpServerRequest httpRequest;
    private final Lazy<String> body;
    private final Lazy<StringTo.Many<String>> params;
    private final Lazy<StringTo.Many<String>> headers;
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
        autoclose = true;
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

    public Iterable<Out2<String, Iterable<String>>> getParams() {
        return params.out1().iterableOut();
    }

    public Iterable<Out2<String, Iterable<String>>> getHeaders() {
        return headers.out1().iterableOut();
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
}
