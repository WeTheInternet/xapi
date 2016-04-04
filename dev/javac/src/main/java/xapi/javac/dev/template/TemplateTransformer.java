package xapi.javac.dev.template;

import com.github.javaparser.ast.visitor.DumpVisitor.SourcePrinter;
import com.github.javaparser.ast.visitor.TransformVisitor.Transformer;
import xapi.util.X_Template;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/4/16.
 */
public class TemplateTransformer extends Transformer {

  @Override
  public String onTemplateStart(
      SourcePrinter printer, String template
  ) {
    printer.printLn(X_Template.class.getCanonicalName() + ".processTemplate(");
    return template;
  }

  @Override
  public void onTemplateEnd(SourcePrinter printer) {
    printer.print(")");
  }
}
