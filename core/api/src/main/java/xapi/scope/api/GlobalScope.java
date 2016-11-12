package xapi.scope.api;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/8/16.
 */
public interface GlobalScope<UserType, RequestType> extends Scope {

  SessionScope<UserType, RequestType> getSessionScope(UserType user);

  @Override
  default Class<? extends Scope> forScope() {
    return xapi.scope.api.GlobalScope.class;
  }
}
