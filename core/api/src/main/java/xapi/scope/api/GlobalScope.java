package xapi.scope.api;

import xapi.util.api.RequestLike;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/8/16.
 */
public interface GlobalScope<UserType, RequestType extends RequestLike> extends Scope {

  SessionScope<UserType, RequestType> getSessionScope(UserType user);

  @Override
  default Class<? extends Scope> forScope() {
    return xapi.scope.api.GlobalScope.class;
  }
}
