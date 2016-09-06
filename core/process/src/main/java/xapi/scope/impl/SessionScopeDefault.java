package xapi.scope.impl;

import xapi.fu.In1Out1;
import xapi.inject.X_Inject;
import xapi.scope.api.Scope.SessionScope;
import xapi.time.X_Time;
import xapi.time.api.Moment;

import java.util.WeakHashMap;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/5/16.
 */
public class SessionScopeDefault <User, Request> extends AbstractScope<SessionScopeDefault<User, Request>> implements SessionScope<User, Request> {

    private User user;
    private Moment activity;
    private WeakHashMap<Request, RequestScope<Request>> requests;
    private In1Out1<Request, RequestScope<Request>> scopeFactory;

    protected SessionScopeDefault() {
        this(req->{
            final RequestScope scope = X_Inject.instance(RequestScope.class);
            scope.setRequest(req);
            return scope;
        });
    }

    protected SessionScopeDefault(In1Out1<Request, RequestScope<Request>> scopeFactory) {
        touch();
        requests = new WeakHashMap<>();
        this.scopeFactory = scopeFactory;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public RequestScope<Request> getRequestScope(Request request) {

        return null;
    }

    @Override
    public SessionScope<User, Request> setUser(User user) {
        this.user = user;
        return this;
    }

    @Override
    public void touch() {
        activity = X_Time.now();
    }
}
