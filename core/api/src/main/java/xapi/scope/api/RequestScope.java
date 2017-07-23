package xapi.scope.api;

import xapi.util.api.RequestLike;

import java.util.Optional;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/8/16.
 */
public interface RequestScope<RequestType extends RequestLike> extends Scope {

    RequestType getRequest();

    void initialize(RequestType req);

    @Override
    default Class<? extends Scope> forScope() {
        return xapi.scope.api.RequestScope.class;
    }

    default <U> SessionScope<U, RequestType> getSession() {
        final Optional<SessionScope> parent = findParent(SessionScope.class, false);
        final SessionScope scope = parent.orElseThrow(() -> new IllegalStateException(getClass() + " must override getSession"));
        return scope;
    }
}
