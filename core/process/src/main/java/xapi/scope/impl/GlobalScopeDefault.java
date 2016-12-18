package xapi.scope.impl;

import xapi.annotation.inject.InstanceDefault;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.fu.In1Out1;
import xapi.inject.X_Inject;
import xapi.scope.api.GlobalScope;
import xapi.scope.api.SessionScope;
import xapi.util.api.HasId;
import xapi.util.api.RequestLike;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/5/16.
 */
@InstanceDefault(implFor = GlobalScope.class)
public class GlobalScopeDefault <User, Request extends RequestLike> extends AbstractScope<GlobalScopeDefault<User, Request>> implements GlobalScope <User, Request> {

    private In1Out1<User, String> keySource;
    private StringTo<SessionScope<User, Request>> users;

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

    @Override
    public SessionScope<User, Request> getSessionScope(User user) {
        String key = keySource.io(user);
        final SessionScope<User, Request> session = users.getOrCreate(key, k -> initUserScope(user, key));
        session.touch();
        return session;
    }

    protected SessionScope<User,Request> initUserScope(User user, String key) {
        SessionScope<User, Request> session = X_Inject.instance(SessionScope.class);
        return session.setUser(user);
    }

}
