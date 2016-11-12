package xapi.server.vertx;

import io.vertx.core.http.HttpServerRequest;
import xapi.collect.api.StringTo;
import xapi.fu.Lazy;
import xapi.fu.Out2;
import xapi.fu.Pointer;
import xapi.util.api.Destroyable;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/3/16.
 */
public class VertxRequest implements Destroyable {

    private HttpServerRequest httpRequest;
    private final Lazy<String> body;
    private final Lazy<StringTo.Many<String>> params;
    private final Lazy<StringTo.Many<String>> headers;

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

    public String getBody() {
        return body.out1();
    }

    public Iterable<Out2<String, Iterable<String>>> getParams() {
        return params.out1().iterableOut();
    }

    public Iterable<Out2<String, Iterable<String>>> getHeaders() {
        return headers.out1().iterableOut();
    }
}
