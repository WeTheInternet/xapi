package xapi.dev.elemental;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.plugin.Transformer;
import com.github.javaparser.ast.plugin.UiTransformer;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;
import xapi.fu.Debuggable;
import xapi.fu.Rethrowable;
import xapi.javac.dev.api.InjectionResolver;
import xapi.javac.dev.api.JavacService;
import xapi.javac.dev.template.TemplateGenerator;
import xapi.source.read.JavaModel.IsType;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/3/16.
 */
public class ElementalTemplateGenerator implements TemplateGenerator, Debuggable, Rethrowable {

  UiTransformer transformer;

  @Override
  public boolean generateTemplate(
      JavacService service,
      CompilationUnitTree cup,
      MethodInvocationTree source,
      IsType type,
      String value,
      InjectionResolver resolver
  ) {
    if (value.trim().startsWith("<")) {
      try {
        final UiContainerExpr expr = JavaParser.parseUiContainer(value);
        final Transformer transformer = getTransformer();
        String src = expr.toSource(transformer);
        resolver.replace(source, src);
      } catch (ParseException e) {
        throw rethrow(e);
      }

    }
    return false;
  }

  protected UiTransformer newTransformer() {
    return new UiTransformer();
  }

  public final UiTransformer getTransformer() {
    if (transformer == null) {
      transformer = newTransformer();
      transformer.setPlugin(new ElementalTemplatePlugin<>());
    }
    return transformer;
  }
}
