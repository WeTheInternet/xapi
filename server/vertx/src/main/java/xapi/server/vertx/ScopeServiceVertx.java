package xapi.server.vertx;

import io.vertx.core.http.HttpServerRequest;
import xapi.annotation.inject.InstanceOverride;
import xapi.fu.Maybe;
import xapi.model.user.ModelUser;
import xapi.scope.request.RequestScope;
import xapi.scope.request.SessionScope;
import xapi.scope.impl.ScopeServiceDefault;
import xapi.scope.service.ScopeService;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/2/16.
 */
@InstanceOverride(implFor = ScopeService.class, priority = 0)
@SuppressWarnings("unchecked")
public class ScopeServiceVertx extends ScopeServiceDefault {

    public RequestScope<VertxRequest, VertxResponse> requestScope(HttpServerRequest req) {
        final SessionScope<ModelUser, VertxRequest, VertxResponse> session = sessionScope();
        // This request will be ignored if the request scope is already initialized
        VertxRequest vertxReq = new VertxRequest(req);
        VertxResponse vertxResp = new VertxResponse(req.response());
        final RequestScopeVertx request = (RequestScopeVertx) session.getRequestScope(
            vertxReq, vertxResp
        );
        if (req != null) {
            request.getRequest().setHttpRequest(req);
        }
        return request;
    }

    public SessionScope<ModelUser, VertxRequest, VertxResponse> sessionScope() {
        return currentScope().getOrCreate(SessionScope.class, c->
            new SessionScopeVertx()
        );
    }
}
