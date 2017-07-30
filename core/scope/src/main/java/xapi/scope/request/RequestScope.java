package xapi.scope.request;

import xapi.fu.Maybe;
import xapi.scope.api.Scope;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/8/16.
 */
public interface RequestScope<RequestType extends RequestLike> extends Scope {

    RequestType getRequest();

    void initialize(RequestType req);

    @Override
    default Class<? extends Scope> forScope() {
        return RequestScope.class;
    }

    default <U> SessionScope<U, RequestType> getSession() {
        final Maybe<SessionScope> parent = findParent(SessionScope.class, false);
        final SessionScope scope = parent.getOrThrow(() -> new IllegalStateException(getClass() + " must override getSession"));
        return scope;
    }
}
