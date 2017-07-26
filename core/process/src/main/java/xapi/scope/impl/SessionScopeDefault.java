package xapi.scope.impl;

import xapi.fu.In1Out1;
import xapi.fu.Maybe;
import xapi.inject.X_Inject;
import xapi.scope.api.RequestScope;
import xapi.scope.api.SessionScope;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.util.api.Destroyable;
import xapi.util.api.RequestLike;

import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/5/16.
 */
public class SessionScopeDefault <User, Request extends RequestLike, Session extends SessionScopeDefault<User, Request, Session>>
    extends AbstractScope<Session> implements SessionScope<User, Request>, Destroyable {

    private User user;
    private Moment activity;
    private In1Out1<Request, RequestScope<Request>> scopeFactory;
    protected final WeakHashMap<Request, RequestScope<Request>> requests;

    protected SessionScopeDefault() {
        this((req)->{
            final RequestScope scope = X_Inject.instance(RequestScope.class);
            scope.initialize(req);
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
    public RequestScope<Request> getRequestScope(Maybe<Request> request) {
        if(request.isPresent()){
            final RequestScope<Request> scope = requests.get(request.get());
            if (scope != null) {
                return scope;
            }
        }
        final Maybe<RequestScope> parentScope = findParentOrSelf(RequestScope.class, false);
        if (parentScope.isPresent()) {
            return parentScope.get();
        }
        // only let one thread look in or mutate this scope at a time
        synchronized (requests) {
            final Request req = request.getOrThrow(() -> new IllegalArgumentException(
                "Cannot get a request scope with a null request from scope " + this
            ));
            final RequestScope<Request> child = requests.get(req);
            if (child != null) {
                // TODO: validate generic signatures.
                return child;
            }
            // no existing scope, create one.
            final RequestScope<Request> newScope = createRequestScope(request);
            final RequestScope was = requests.put(req, newScope);
            if (was != null) {
                was.release();
            }
            return newScope;
        }
    }

    protected RequestScope<Request> createRequestScope(Maybe<Request> request) {
        final RequestScope<Request> scope = new RequestScopeDefault<>();
        if (request.isPresent()) {
            scope.initialize(request.get());
        }
        return scope;
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

    @Override
    public void destroy() {
        requests.clear();
        user = null;
        scopeFactory = null;
    }
}
