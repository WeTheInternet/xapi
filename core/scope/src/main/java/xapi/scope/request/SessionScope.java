package xapi.scope.request;

import xapi.fu.Maybe;
import xapi.scope.api.Scope;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/8/16.
 */
public interface SessionScope<UserType, RequestType extends RequestLike, ResponseType extends ResponseLike> extends Scope {
  UserType getUser();

  RequestScope<RequestType, ResponseType> getRequestScope(RequestType request, ResponseType response);

  SessionScope<UserType, RequestType, ResponseType> setUser(UserType user);

  void touch();

  @Override
  default Class<? extends Scope> forScope() {
    return SessionScope.class;
  }
}
