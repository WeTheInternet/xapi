package xapi.server.vertx;

import xapi.annotation.inject.InstanceOverride;
import xapi.scope.api.RequestScope;
import xapi.scope.api.SessionScope;
import xapi.scope.impl.SessionScopeDefault;

import java.util.Optional;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/2/16.
 */
@InstanceOverride(implFor = SessionScope.class)
public class SessionScopeVertx extends
    SessionScopeDefault<CollideUser, VertxRequest, SessionScopeVertx>{

    private final ScopeServiceVertx service;
    private CollideUser user;

    public SessionScopeVertx(ScopeServiceVertx service) {
        this.service = service;
    }

    @Override
    public CollideUser getUser() {
        return user;
    }

    @Override
    public RequestScope<VertxRequest> getRequestScope(Optional<VertxRequest> request) {
        synchronized (requests) {
            RequestScopeVertx current = (RequestScopeVertx)get(RequestScope.class);
            if (current == null) {
                current = new RequestScopeVertx(request.get());
                setLocal(RequestScope.class, current);
            } else {
                if (request.isPresent()) {
                    current.initialize(request.get());
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
