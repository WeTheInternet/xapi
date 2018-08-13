package xapi.scope.request;

import xapi.annotation.process.Multiplexed;
import xapi.scope.api.Scope;
import xapi.scope.spi.RequestLike;
import xapi.scope.spi.ResponseLike;
import xapi.time.X_Time;
import xapi.time.api.Moment;

/**
 * You should ensure that any implementing class is annotated with @Multiplexed
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/8/16.
 */
@Multiplexed
public interface SessionScope<UserType, RequestType extends RequestLike, ResponseType extends ResponseLike> extends Scope {

  UserType getUser();

  RequestScope<RequestType, ResponseType> getRequestScope(RequestType request, ResponseType response);

  /**
   * TODO: consider adding a request id/key to this method (we can easily have one in scope w/ current code,
   * and it could be useful in implementing some single-use connection keys).
   */
  SessionScope<UserType, RequestType, ResponseType> setUser(UserType user);

  void touch();

  Moment lastTouch();

  @Override
  default Class<? extends Scope> forScope() {
    return SessionScope.class;
  }

  default boolean isExpired(double sessionTTL) {
    return lastTouch().compareTo(X_Time.now().minus(sessionTTL)) < 0;
  }
}
