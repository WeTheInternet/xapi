package xapi.scope.api;

/**
 * Created by James X. Nelson (james @wetheinter.net) on 10/8/16.
 */
public interface LocalScope extends Scope {
  @Override
  default Class<? extends Scope> forScope() {
    return xapi.scope.api.LocalScope.class;
  }
}
