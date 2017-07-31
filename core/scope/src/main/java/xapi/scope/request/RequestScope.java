package xapi.scope.request;

import xapi.fu.Maybe;
import xapi.scope.api.Scope;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/8/16.
 */
public interface RequestScope<RequestType extends RequestLike, ResponseType extends ResponseLike> extends Scope {

    RequestType getRequest();

    ResponseType getResponse();

    void initialize(RequestType req, ResponseType resp);

    @Override
    default Class<? extends Scope> forScope() {
        return RequestScope.class;
    }

    default <U> SessionScope<U, RequestType, ResponseType> getSession() {
        final Maybe<SessionScope> parent = findParent(SessionScope.class, false);
        final SessionScope scope = parent.getOrThrow(() -> new IllegalStateException(getClass() + " must override getSession"));
        return scope;
    }

    default String getPath() {
        return getRequest().getPath();
    }
}
