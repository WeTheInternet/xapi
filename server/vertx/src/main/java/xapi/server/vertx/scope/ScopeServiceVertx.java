package xapi.server.vertx.scope;

import io.vertx.core.http.HttpServerRequest;
import xapi.annotation.inject.InstanceOverride;
import xapi.fu.In1.In1Unsafe;
import xapi.model.user.ModelUser;
import xapi.scope.api.GlobalScope;
import xapi.scope.api.Scope;
import xapi.scope.impl.ScopeServiceDefault;
import xapi.scope.request.RequestScope;
import xapi.scope.request.SessionScope;
import xapi.scope.service.ScopeService;
import xapi.server.vertx.VertxRequest;
import xapi.server.vertx.VertxResponse;

import java.util.concurrent.TimeUnit;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/2/16.
 */
@InstanceOverride(implFor = ScopeService.class, priority = 0)
@SuppressWarnings("unchecked")
public class ScopeServiceVertx extends ScopeServiceDefault {

    @SuppressWarnings("unused") // used by collIDE (until it gets revamped w/ new code)
    public RequestScope<VertxRequest, VertxResponse> requestScope(HttpServerRequest ctx) {
        final SessionScope<ModelUser, VertxRequest, VertxResponse> session = sessionScope();
        // This request will be ignored if the request scope is already initialized
        // the Content-Type header is lazy / bad...
        VertxRequest vertxReq = new VertxRequest(ctx, ctx.getHeader("Content-Type"));
        VertxResponse vertxResp = new VertxResponse(ctx.response());
        final RequestScope<VertxRequest, VertxResponse> request = session.getRequestScope(
            vertxReq, vertxResp
        );
        request.getRequest().setHttpRequest(ctx);
        return request;
    }

    public SessionScope<ModelUser, VertxRequest, VertxResponse> sessionScope() {
        return currentScope().getOrCreate(SessionScope.class, c->
            new SessionScopeVertx()
        );
    }

    @Override
    protected <S extends Scope> void maybeRelease(S scope) {
        if (scope.forScope() == SessionScope.class) {
            SessionScope s = (SessionScope) scope;
            // TODO: check if session needs to be persisted /
            // / deltas resolved with other instances,
            if (s.isExpired(getSessionTTL())) {
                s.release();
            }
            return;
        }
        super.maybeRelease(scope);
    }

    protected double getSessionTTL() {
        return TimeUnit.HOURS.toMillis(24); // obscenely large while we're developing...
    }

    public void globalScopeVertx(In1Unsafe<GlobalScopeVertx> todo) {
        globalScope(todo);
    }

    @Override
    public <G extends GlobalScope> void globalScope(
        In1Unsafe<G> todo
    ) {
        super.globalScope(todo);
    }

}
