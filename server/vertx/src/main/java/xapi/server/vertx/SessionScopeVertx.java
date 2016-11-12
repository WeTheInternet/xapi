package xapi.server.vertx;

import xapi.scope.api.RequestScope;
import xapi.scope.api.SessionScope;
import xapi.scope.impl.SessionScopeDefault;

import java.util.Optional;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/2/16.
 */
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
            RequestScope<VertxRequest> current = get(RequestScope.class);
            if (current == null) {
                current = new RequestScopeVertx(request.get());
                setLocal(RequestScope.class, current);
            } else {
                if (request.isPresent()) {
                    current.setRequest(request.get());
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
