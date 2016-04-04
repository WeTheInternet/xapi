package xapi.javac.dev.api;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/3/16.
 */
public interface MagicMethodInjector {

  default void init(JavacService service) {

  }

  boolean performInjection(
      CompilationUnitTree cup, MethodInvocationTree source, InjectionResolver resolver
  );
}
