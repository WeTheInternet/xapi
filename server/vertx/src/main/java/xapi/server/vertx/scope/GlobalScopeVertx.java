package xapi.server.vertx.scope;

import io.vertx.ext.auth.User;
import xapi.annotation.inject.InstanceOverride;
import xapi.model.user.ModelUser;
import xapi.scope.api.GlobalScope;
import xapi.scope.impl.GlobalScopeDefault;
import xapi.server.vertx.VertxContext;
import xapi.server.vertx.VertxRequest;
import xapi.server.vertx.VertxResponse;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 8/11/18.
 */
@InstanceOverride(implFor = GlobalScope.class)
public class GlobalScopeVertx extends GlobalScopeDefault <User, VertxRequest, VertxResponse, SessionScopeVertx, VertxContext> {

    public GlobalScopeVertx() {
        setUserKeyExtractor(u->u.principal().getString("username"));
    }

    @Override
    protected Class<SessionScopeVertx> sessionType() {
        return SessionScopeVertx.class;
    }

    @Override
    public SessionScopeVertx findSession(String key) {
        return super.findSession(key);
    }
}
