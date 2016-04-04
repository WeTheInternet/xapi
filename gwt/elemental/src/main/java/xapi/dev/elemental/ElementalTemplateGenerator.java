package xapi.dev.elemental;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;
import xapi.javac.dev.api.InjectionResolver;
import xapi.javac.dev.api.JavacService;
import xapi.javac.dev.template.TemplateGenerator;
import xapi.source.read.JavaModel.IsType;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/3/16.
 */
public class ElementalTemplateGenerator implements TemplateGenerator {

  @Override
  public boolean generateTemplate(
      JavacService service,
      CompilationUnitTree cup,
      MethodInvocationTree source,
      IsType type,
      String value,
      InjectionResolver resolver
  ) {
    return false;
  }
}
