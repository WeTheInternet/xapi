package xapi.server.vertx.scope;

import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import xapi.annotation.inject.InstanceOverride;
import xapi.annotation.process.Multiplexed;
import xapi.model.user.ModelUser;
import xapi.scope.request.RequestScope;
import xapi.scope.request.SessionScope;
import xapi.scope.impl.SessionScopeDefault;
import xapi.server.model.ModelSession;
import xapi.server.vertx.VertxRequest;
import xapi.server.vertx.VertxResponse;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/2/16.
 */
@Multiplexed
@InstanceOverride(implFor = SessionScope.class)
public class SessionScopeVertx extends
    SessionScopeDefault<User, VertxRequest, VertxResponse, SessionScopeVertx>{

    private User user;
    private ModelSession model;
    private RoutingContext context;

    public SessionScopeVertx() {
        System.out.println("New Session Scope " + this);
    }

    @Override
    public User getUser() {
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
    public SessionScope<User, VertxRequest, VertxResponse> setUser(User user) {
        assert this.user == null || this.user.equals(user) : "Cannot change users...";
        this.user = user;

        return this;
    }

    public void setModel(ModelSession model) {
        this.model = model;
    }

    public ModelSession getModel() {
        return model;
    }

    @Override
    public void touch() {
        if (context != null) {
            context.session().setAccessed();
        }
        super.touch();
    }

    public void setContext(RoutingContext context) {
        this.context = context;
    }

    public RoutingContext getContext() {
        return context;
    }
}
