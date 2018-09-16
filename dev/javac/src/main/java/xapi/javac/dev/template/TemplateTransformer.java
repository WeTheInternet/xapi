package xapi.javac.dev.template;

import com.github.javaparser.ast.expr.TemplateLiteralExpr;
import com.github.javaparser.ast.plugin.Transformer;
import xapi.fu.Printable;
import xapi.util.X_Template;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/4/16.
 */
public class TemplateTransformer extends Transformer {

  public TemplateTransformer() {
    setShouldQuote(true);
  }

  @Override
  public String onTemplateStart(
      Printable printer, TemplateLiteralExpr template
  ) {
    printer.println(X_Template.class.getCanonicalName() + ".processTemplate(");
    return template.getValue();
  }

  @Override
  public void onTemplateEnd(Printable printer) {
    printer.print(")");
  }
}
