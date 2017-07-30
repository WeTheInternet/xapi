package xapi.server.vertx;

import xapi.annotation.inject.InstanceOverride;
import xapi.fu.Maybe;
import xapi.scope.request.RequestScope;
import xapi.scope.request.SessionScope;
import xapi.scope.impl.SessionScopeDefault;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/2/16.
 */
@InstanceOverride(implFor = SessionScope.class)
public class SessionScopeVertx extends
    SessionScopeDefault<CollideUser, VertxRequest, SessionScopeVertx>{

    private CollideUser user;

    public SessionScopeVertx() {
        System.out.println();
    }

    @Override
    public CollideUser getUser() {
        return user;
    }

    @Override
    public RequestScope<VertxRequest> getRequestScope(Maybe<VertxRequest> request) {
        synchronized (requests) {
            RequestScopeVertx current = (RequestScopeVertx)get(RequestScope.class);
            if (current == null) {
                current = new RequestScopeVertx(request.get());
                current.setParent(this);
                setLocal(RequestScope.class, current);
            } else {
                if (request.isPresent()) {
                    final VertxRequest req = request.get();
                    assert current.findParent(SessionScope.class, false).get() == SessionScopeVertx.this;
                    current.initialize(req);
                }
            }
            return current;
        }
    }

    @Override
    public SessionScope<CollideUser, VertxRequest> setUser(CollideUser user) {
        this.user = user;
        return this;
    }

    @Override
    public void touch() {

    }
}
