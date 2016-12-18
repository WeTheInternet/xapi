package xapi.server.vertx;

import io.vertx.core.http.HttpServerRequest;
import xapi.annotation.inject.InstanceOverride;
import xapi.scope.api.RequestScope;
import xapi.scope.api.SessionScope;
import xapi.scope.impl.ScopeServiceDefault;
import xapi.scope.service.ScopeService;

import java.util.Optional;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/2/16.
 */
@InstanceOverride(implFor = ScopeService.class, priority = 0)
@SuppressWarnings("unchecked")
public class ScopeServiceVertx extends ScopeServiceDefault {

    public RequestScope<VertxRequest> requestScope(HttpServerRequest req) {
        final SessionScope<CollideUser, VertxRequest> session = sessionScope();
        // This request will be ignored if the request scope is already initialized
        VertxRequest vertxReq = new VertxRequest(req);
        final RequestScopeVertx request = (RequestScopeVertx) session.getRequestScope(
            Optional.of(vertxReq)
        );
        if (req != null) {
            request.getRequest().setHttpRequest(req);
        }
        return request;
    }

    public SessionScope<CollideUser, VertxRequest> sessionScope() {
        return currentScope().getOrCreate(SessionScope.class, c->
            new SessionScopeVertx(this)
        );
    }
}
