package xapi.server.vertx.scope;

import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import xapi.annotation.inject.InstanceOverride;
import xapi.annotation.process.Multiplexed;
import xapi.log.X_Log;
import xapi.scope.impl.SessionScopeDefault;
import xapi.scope.request.RequestScope;
import xapi.scope.request.SessionScope;
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

    // TODO: VertxModelSession which implements ClusterSerializable
    private ModelSession model;
    private transient RoutingContext context;

    public SessionScopeVertx() {
        // This class is not serializable and likely never will be.
        // So, instead, we should have a subset of serializable values
        // which we will persist and reload, so we can have many SessionScope instances,
        // but each one wraps the same subset of data.
        X_Log.info(ScopeServiceVertx.class, "New Session Scope ", this);
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
