package xapi.javac.dev.template;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;
import xapi.javac.dev.api.InjectionResolver;
import xapi.javac.dev.api.JavacService;
import xapi.source.read.JavaModel.IsType;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/3/16.
 */
public interface TemplateGenerator {

  boolean generateTemplate(
      JavacService service,
      CompilationUnitTree cup,
      MethodInvocationTree source,
      IsType type,
      String value,
      InjectionResolver resolver
  );
}
