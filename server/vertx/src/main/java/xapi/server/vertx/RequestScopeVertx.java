package xapi.server.vertx;

import io.vertx.core.http.HttpServerRequest;
import xapi.fu.Out1;
import xapi.fu.Out2;
import xapi.scope.api.RequestScope;
import xapi.scope.impl.AbstractScope;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/2/16.
 */
public class RequestScopeVertx extends AbstractScope<RequestScopeVertx> implements RequestScope <VertxRequest> {

    private VertxRequest req;

    public RequestScopeVertx(VertxRequest req) {
        setRequest(req);
    }

    @Override
    public VertxRequest getRequest() {
        return req;
    }

    @Override
    public String getPath() {
        return getHttpRequest().path();
    }

    @Override
    public String getBody() {
        return req.getBody();
    }

    @Override
    public String getParam(String name, Out1<String> dflt) {
        String param = req.getHttpRequest().getParam(name);
        return param == null ? dflt.out1() : param;
    }

    @Override
    public String getHeader(String name, Out1<String> dflt) {
        String header = req.getHttpRequest().getHeader(name);
        return header == null ? dflt.out1() : header;
    }

    @Override
    public Iterable<Out2<String, Iterable<String>>> getParams() {
        return req.getParams();
    }

    @Override
    public Iterable<Out2<String, Iterable<String>>> getHeaders() {
        return req.getHeaders();
    }

    public HttpServerRequest getHttpRequest() {
        return req.getHttpRequest();
    }

    @Override
    public void setRequest(VertxRequest req) {
        if (this.req != null && this.req != req) {
            this.req.destroy();
        }
        this.req = req;
        preload(req);
    }

    protected void preload(VertxRequest req) {

    }
}
