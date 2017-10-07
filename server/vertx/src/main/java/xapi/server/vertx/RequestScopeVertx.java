package xapi.server.vertx;

import xapi.annotation.inject.InstanceOverride;
import xapi.annotation.process.Multiplexed;
import xapi.scope.request.RequestScope;
import xapi.scope.impl.AbstractScope;

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

    protected void preload(VertxRequest req, VertxResponse resp) {

    }
}
