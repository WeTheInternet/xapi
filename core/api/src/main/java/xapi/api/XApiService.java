package xapi.api;

import xapi.fu.In1.In1Unsafe;
import xapi.fu.OutMany;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 2/14/16.
 */
public interface XApiService {

  <S extends Scope> void runInScope(S scope, In1Unsafe<S> todo);

  <S extends Scope> void runInNewScope(Class<S> scope, In1Unsafe<S> todo);

  Scope currentScope();

  <T> T create(Class<T> cls, OutMany args);


  // TODO: create a .runInEnvironment(boolean inheritEnviro, ClassLoader cl);
  // except, instead of ClassLoader, use some other mechanism to define classpath,
  // preferably by sending Class ... classesAnnotatedWithDependencies
}
