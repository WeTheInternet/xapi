package xapi.server.vertx;

import xapi.annotation.inject.InstanceOverride;
import xapi.annotation.process.Multiplexed;
import xapi.fu.Maybe;
import xapi.scope.request.RequestLike;
import xapi.scope.request.RequestScope;
import xapi.scope.request.ResponseLike;
import xapi.scope.request.SessionScope;
import xapi.scope.impl.SessionScopeDefault;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/2/16.
 */
@Multiplexed
@InstanceOverride(implFor = SessionScope.class)
public class SessionScopeVertx extends
    SessionScopeDefault<CollideUser, VertxRequest, VertxResponse, SessionScopeVertx>{

    private CollideUser user;

    public SessionScopeVertx() {
        System.out.println();
    }

    @Override
    public CollideUser getUser() {
        return user;
    }

    @Override
    public RequestScope<VertxRequest, VertxResponse> getRequestScope(VertxRequest req, VertxResponse resp) {
        synchronized (requests) {
            RequestScopeVertx current = (RequestScopeVertx)get(RequestScope.class);
            if (current == null) {
                current = new RequestScopeVertx(req, resp);
                // egregious hack... no time to fix before demo
                released = false;
                current.setParent(this);
                setLocal(RequestScope.class, current);
            } else {
                if (req != null) {
                    assert current.findParent(SessionScope.class, false).get() == SessionScopeVertx.this;
                    current.initialize(req, resp);
                }
            }
            return current;
        }
    }

    @Override
    public SessionScope<CollideUser, VertxRequest, VertxResponse> setUser(CollideUser user) {
        this.user = user;
        return this;
    }

    @Override
    public void touch() {

    }
}
