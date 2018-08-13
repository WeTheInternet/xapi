package xapi.scope.api;

import xapi.fu.In1Out1.In1Out1Unsafe;
import xapi.scope.request.SessionScope;

/**
 * A special singleton scope that we create on demand as the ultimate parent of all scopes.
 *
 * Try hard not to store objects which themselves store many objects here.
 *
 * For any process with a known end that you can use to release a scope,
 * you should instead make your own scope class,
 * and add-and-remove it from your runtime parent scope:
 *
 * Scope s = X_Scope.currentScope(); // or wherever you want to supply a parent
 * MyScope myScope = s.findParentOrCreateChild(MyScope.class, true, MyScope::new);
 *
 * Using this helper method will automatically detach MyScope whenever
 * its parent is detached.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 10/8/16.
 */
public interface GlobalScope <Session extends SessionScope> extends Scope {

  @Override
  default Class<? extends Scope> forScope() {
    return xapi.scope.api.GlobalScope.class;
  }

  Session findSession(String key);
  Session findOrCreateSession(String key, In1Out1Unsafe<String, Session> factory);
}
