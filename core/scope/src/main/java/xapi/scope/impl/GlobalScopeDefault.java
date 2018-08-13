package xapi.scope.impl;

import xapi.annotation.inject.InstanceDefault;
import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.fu.In1Out1;
import xapi.fu.In1Out1.In1Out1Unsafe;
import xapi.inject.X_Inject;
import xapi.scope.api.GlobalScope;
import xapi.scope.request.SessionScope;
import xapi.scope.spi.RequestContext;
import xapi.scope.spi.RequestLike;
import xapi.scope.spi.ResponseLike;
import xapi.util.api.HasId;

/**
 * A generic implementation of a {@link GlobalScope}.
 *
 * You should override {@link GlobalScopeDefault#sessionType()} with your actual session type.
 *
 * We will try to reduce the type parameter spam here, but keep in mind that you should not
 * really reference this type directly in your code; either use the GlobalScope interface,
 * or extend this type with something that supplies your actual type parameters,
 * and then use that type (which shouldn't have type parameter spam)
 *
 * Created by James X. Nelson (james @wetheinter.net) on 9/5/16.
 */
@InstanceDefault(implFor = GlobalScope.class)
public class GlobalScopeDefault <
    User,
    Request extends RequestLike,
    Response extends ResponseLike,
    Session extends SessionScope<User, Request, Response>,
    Context extends RequestContext<User, Request, Response, Session>
    >
    extends AbstractScope<GlobalScopeDefault<User, Request, Response, Session, Context>>
    implements GlobalScope<Session> {

    private In1Out1<User, String> keySource;
    private StringTo<Session> users;

    public GlobalScopeDefault() {
        keySource = u-> {
            if (u instanceof HasId) {
                return ((HasId)u).getId();
            }
            assert false : "Cannot extract an id from user " + u + " of type " + (u == null ? null : u.getClass())
                +"\nYou should extend GlobalScopeDefault with an @InstanceOverride to handle your user type";

            throw new IllegalStateException();
        };
        users = createSessionMap();
    }

    /**
     * Allows you to create a session map of your choosing;
     * while implementing the interface might be asking a little much,
     * this does allow you to add a distributed collection at your will.
     *
     * TODO: change type to MapLike...
     */
    protected StringTo<Session> createSessionMap() {
        return X_Collect.newStringMap(sessionType(), X_Collect.MUTABLE_CONCURRENT);
    }

    @SuppressWarnings("unchecked")
    protected Class<Session> sessionType() {
        return Class.class.cast(SessionScopeDefault.class);
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

    @SuppressWarnings("unchecked")
    protected Session initUserScope(User user, String key) {
        SessionScope<User, Request, Response> session = X_Inject.instance(SessionScope.class);
        session.setUser(user);
        return (Session) session;
    }

    @Override
    protected void onRelease() {
        super.onRelease();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Session findSession(String key) {
        return users.get(key);
    }

    @Override
    public Session findOrCreateSession(String key, In1Out1Unsafe<String, Session> factory) {
        return users.getOrCreate(key, factory);
    }
}
