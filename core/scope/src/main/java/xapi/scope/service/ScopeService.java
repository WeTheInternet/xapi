package xapi.scope.service;

import xapi.fu.Do;
import xapi.fu.In1.In1Unsafe;
import xapi.fu.In2.In2Unsafe;
import xapi.scope.api.GlobalScope;
import xapi.scope.api.Scope;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/14/16.
 */
public interface ScopeService {

  default <S extends Scope> void runInScope(S scope, In1Unsafe<S> todo) {
    runInScope(scope, (s, onDone)->{
      try {
        todo.in(s);
      } finally {
        onDone.done();
      }
    });
  }

  <S extends Scope> void runInScope(S scope, In2Unsafe<S, Do> todo);

  default <S extends Scope> void runInNewScope(Class<S> scope, In1Unsafe<S> todo) {
    runInNewScope(scope, (s, onDone)->{
      try {
        todo.in(s);
      } finally {
        onDone.done();
      }
    });
  }

  <S extends Scope, Generic extends S> void runInNewScope(Class<Generic> scope, In2Unsafe<Generic, Do> todo);

  Scope currentScope();

  Do inheritScope();

  default <G extends GlobalScope> void globalScope(In1Unsafe<G> todo) {
    final Class<G> cls = Class.class.cast(GlobalScope.class);
    runInNewScope(cls, todo);
  }

    // TODO: create a .runInEnvironment(boolean inheritEnviro, ClassLoader cl);
  // except, instead of ClassLoader, use some other mechanism to define classpath,
  // preferably by sending Class ... classesAnnotatedWithDependencies
}
