package xapi.scope.spi;

import xapi.scope.request.RequestScope;
import xapi.scope.request.SessionScope;

/**
 * This ugly pile of type parameters are here,
 * so that you can reference a HasRequestContext without piles of "type parameter soup".
 *
 * All type parameters to this interface belong in a runtime set,
 * and will interact with each other fairly intimately.
 *
 * If you find yourself referencing this type instead of HasRequestContext,
 * you should try to rethink your design decisions (and instead pass the wrapper type,
 * or directly pass the object / scopes you want from the context).
 *
 * @param <User>
 * @param <Request>
 * @param <Response>
 * @param <Session>
 */
public interface RequestContext<
    User,
    Request extends RequestLike,
    Response extends ResponseLike,
    Session extends SessionScope<User, Request, Response>,
    Scope extends RequestScope<Request, Response>
> {
    User getUser();

    Request getRequest();

    Response getResponse();

    Scope getScope();

    Session getSession();

    Integer finish(Throwable failure);

    boolean isFinished();
}
