package xapi.scope.impl;

import xapi.fu.In1Out1;
import xapi.fu.In2Out1;
import xapi.fu.Maybe;
import xapi.inject.X_Inject;
import xapi.scope.X_Scope;
import xapi.scope.request.RequestScope;
import xapi.scope.request.ResponseLike;
import xapi.scope.request.SessionScope;
import xapi.time.X_Time;
import xapi.time.api.Moment;
import xapi.util.api.Destroyable;
import xapi.scope.request.RequestLike;

import java.util.WeakHashMap;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 9/5/16.
 */
public class SessionScopeDefault <User, Request extends RequestLike, Response extends ResponseLike, Session extends SessionScopeDefault<User, Request, Response, Session>>
    extends AbstractScope<Session> implements SessionScope<User, Request, Response>, Destroyable {

    private User user;
    private Moment activity;
    private In2Out1<Request, Response, RequestScope<Request, Response>> scopeFactory;
    protected final WeakHashMap<Request, RequestScope<Request, Response>> requests;

    protected SessionScopeDefault() {
        this((req, resp)->{
            final RequestScope scope = X_Inject.instance(RequestScope.class);
            scope.initialize(req, resp);
            return scope;
        });
    }

    protected SessionScopeDefault(In2Out1<Request, Response, RequestScope<Request, Response>> scopeFactory) {
        touch();
        requests = new WeakHashMap<>();
        this.scopeFactory = scopeFactory;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public RequestScope<Request, Response> getRequestScope(Request req, Response resp) {
        final RequestScope<Request, Response> scope = requests.get(req);
        if (scope != null) {
            return scope;
        }
        final Maybe<RequestScope> parentScope = findParentOrSelf(RequestScope.class, false);
        if (parentScope.isPresent()) {
            return parentScope.get();
        }
        // only let one thread look in or mutate this scope at a time
        synchronized (requests) {
            final RequestScope<Request, Response> child = requests.get(req);
            if (child != null) {
                // TODO: validate generic signatures.
                return child;
            }
            // no existing scope, create one.
            final RequestScope<Request, Response> newScope = createRequestScope(req, resp);
            final RequestScope was = requests.put(req, newScope);
            if (was != null) {
                was.release();
            }
            newScope.setParent(X_Scope.currentScope());
            return newScope;
        }
    }

    protected RequestScope<Request, Response> createRequestScope(Request request, Response response) {
        final RequestScope<Request, Response> scope = new RequestScopeDefault<>();
        scope.initialize(request, response);
        return scope;
    }

    @Override
    public SessionScope<User, Request, Response> setUser(User user) {
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
