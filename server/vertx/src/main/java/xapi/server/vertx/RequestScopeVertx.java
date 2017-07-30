package xapi.server.vertx;

import xapi.annotation.inject.InstanceOverride;
import xapi.scope.request.RequestScope;
import xapi.scope.impl.AbstractScope;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/2/16.
 */
@InstanceOverride(implFor = RequestScope.class)
public class RequestScopeVertx extends AbstractScope<RequestScopeVertx> implements RequestScope <VertxRequest> {

    private VertxRequest req;

    public RequestScopeVertx() {
    }

    public RequestScopeVertx(VertxRequest req) {
        this.req = req;
    }

    @Override
    public VertxRequest getRequest() {
        return req;
    }

    @Override
    public void initialize(VertxRequest request) {
        if (this.req != null && this.req != request) {
            this.req.destroy();
        }
        this.req = request;
        preload(request);

    }

    protected void preload(VertxRequest req) {

    }
}
