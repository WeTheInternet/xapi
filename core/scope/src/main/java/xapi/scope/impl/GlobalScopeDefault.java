package xapi.scope.impl;

import xapi.annotation.inject.InstanceDefault;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.fu.In1Out1;
import xapi.inject.X_Inject;
import xapi.scope.api.GlobalScope;
import xapi.scope.request.ResponseLike;
import xapi.scope.request.SessionScope;
import xapi.util.api.HasId;
import xapi.scope.request.RequestLike;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/5/16.
 */
@InstanceDefault(implFor = GlobalScope.class)
public class GlobalScopeDefault <User, Request extends RequestLike, Response extends ResponseLike> extends AbstractScope<GlobalScopeDefault<User, Request, Response>> implements GlobalScope {

    private In1Out1<User, String> keySource;
    private StringTo<SessionScope<User, Request, Response>> users;

    public GlobalScopeDefault() {
        users = X_Collect.newStringMap(SessionScope.class, X_Collect.MUTABLE_CONCURRENT);
        keySource = u-> {
            if (u instanceof HasId) {
                return ((HasId)u).getId();
            }
            assert false : "Cannot extract an id from user " + u + " of type " + (u == null ? null : u.getClass())
                +"\nYou should extend GlobalScopeDefault with an @InstanceOverride to handle your user type";

            throw new IllegalStateException();
        };
    }

    protected void setUserKeyExtractor(In1Out1<User, String> keySource) {
        this.keySource = keySource;
    }

    public SessionScope<User, Request, Response> getSessionScope(User user) {
        String key = keySource.io(user);
        final SessionScope<User, Request, Response> session = users.getOrCreate(key, k -> initUserScope(user, key));
        session.touch();
        return session;
    }

    protected SessionScope<User, Request, Response> initUserScope(User user, String key) {
        SessionScope<User, Request, Response> session = X_Inject.instance(SessionScope.class);
        return session.setUser(user);
    }

}
