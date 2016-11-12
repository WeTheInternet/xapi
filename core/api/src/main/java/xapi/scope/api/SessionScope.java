package xapi.scope.api;

import java.util.Optional;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/8/16.
 */
public interface SessionScope<UserType, RequestType> extends Scope {
  UserType getUser();

  RequestScope<RequestType> getRequestScope(Optional<RequestType> request);

  xapi.scope.api.SessionScope<UserType, RequestType> setUser(UserType user);

  void touch();

  @Override
  default Class<? extends Scope> forScope() {
    return xapi.scope.api.SessionScope.class;
  }
}
