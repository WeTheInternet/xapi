package xapi.server.vertx.scope;

import xapi.annotation.inject.InstanceOverride;
import xapi.annotation.process.Multiplexed;
import xapi.scope.impl.AbstractScope;
import xapi.scope.request.RequestScope;
import xapi.scope.request.SessionScope;
import xapi.server.vertx.VertxRequest;
import xapi.server.vertx.VertxResponse;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/2/16.
 */
@InstanceOverride(implFor = RequestScope.class)
@Multiplexed
public class RequestScopeVertx extends AbstractScope<RequestScopeVertx> implements RequestScope <VertxRequest, VertxResponse> {

    private VertxRequest req;
    private VertxResponse resp;

    public RequestScopeVertx() {
    }

    public RequestScopeVertx(VertxRequest req, VertxResponse resp) {
        this.req = req;
        this.resp = resp;
    }

    @Override
    public VertxRequest getRequest() {
        return req;
    }

    @Override
    public VertxResponse getResponse() {
        return resp;
    }

    @Override
    public void initialize(VertxRequest req, VertxResponse resp) {
        if (this.req != null && this.req != req) {
            this.req.destroy();
        }
        if (this.resp != null && this.resp != resp) {
            this.resp.destroy();
        }
        this.req = req;
        this.resp = resp;
        preload(req, resp);

    }

    @Override
    public String getProtocol() {
        return "http" + (req.getHttpRequest().isSSL() ? "s" : "") + "://";
    }

    @Override
    public String getHost() {
        return req.getHttpRequest().host();
    }

    public SessionScopeVertx session() {
        final SessionScope session = getSession();
        return (SessionScopeVertx) session;
    }

    protected void preload(VertxRequest req, VertxResponse resp) {

    }

    @Override
    public String toString() {
        return "RequestScopeVertx{" +
            "req=" + req.toString() +
            ", resp=" + resp +
            "} ";
    }
}
