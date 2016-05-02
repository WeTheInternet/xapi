package xapi.dev.elemental;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LiteralExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TemplateLiteralExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiBodyExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.expr.UiExpr;
import com.github.javaparser.ast.plugin.Transformer;
import com.github.javaparser.ast.plugin.UiTranslatorPlugin;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import xapi.fu.Printable;
import xapi.source.X_Source;
import xapi.util.api.DebugRethrowable;

import java.util.Optional;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/16/16.
 */
public class ElementalTemplatePlugin <Ctx> implements UiTranslatorPlugin, DebugRethrowable {

  protected class ElementalUiVisitor extends VoidVisitorAdapter <Ctx> {

    private final Expression expr;
    private Printable printer;
    private boolean hasRoot;

    public ElementalUiVisitor(UiExpr expr) {
      if (expr instanceof TemplateLiteralExpr) {
        String src = ((TemplateLiteralExpr)expr).getValueWithoutTicks();
        try {
          this.expr = JavaParser.parseExpression(src);
        } catch (ParseException e) {
          throw rethrow(e);
        }
      } else {
        this.expr = expr;
      }
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Ctx arg) {
      final Optional<AnnotationExpr> uiAnno = n.getAnnotations().stream()
          .filter(anno -> "Ui".equals(anno.getName().getSimpleName()))
          .findFirst();
      if (uiAnno.isPresent()) {
        final AnnotationExpr a = uiAnno.get();
      }
      super.visit(n, arg);
    }

    @Override
    public void visit(UiContainerExpr n, Ctx arg) {
      boolean isRoot = !hasRoot;
      if (isRoot) {
        hasRoot = true;
        printer.print("new PotentialElement(\"");
      } else {
        printer.indent();
        printer.println();
        printer.println();
        printer.print(".createChild(\"");
      }
      printer.print(n.getName());
      printer.print("\")");
      super.visit(n, arg);
      printer.println();
      if (isRoot) {

        printer.print(".build()");
      } else {
        printer.println(".finishChild()");
        printer.outdent();
      }
    }

    @Override
    public void visit(UiAttrExpr n, Ctx arg) {
      printer.indent();
      printer.println();
      printer.print(".set(\"");
      printer.print(n.getName().getName());
      printer.print("\", ");
      final Expression value = n.getExpression();
      if (value instanceof LiteralExpr) {
        // literals can be written directly as source.
        if (value instanceof StringLiteralExpr) {
          String asString = ((StringLiteralExpr)value).getValue();
          printer.print("\"");
          // Do full generator escaping here...
          printer.print(asString); // ...later
          printer.print("\"");
        }
      // non-literals...  time to map some stuff!
      } else if (value instanceof NameExpr){
        String name = ((NameExpr)value).getName();
        if (name.startsWith("$")) {
          // A $dollar reference!
          // We'll want to emit a magic method to handle type coercions in compiler...
          printer.print("xapi.fu.X_Fu.coerce(" + name.substring(1) +")");
        }
      }
      printer.print(")");
      printer.outdent();
//      super.visit(n, arg); // leave this disabled until nesting is considered
    }

    @Override
    public void visit(UiBodyExpr n, Ctx arg) {
      n.getChildren().forEach(uiExpr -> {
        if (uiExpr instanceof TemplateLiteralExpr) {
          final String src = ((TemplateLiteralExpr)uiExpr).getValueWithoutTicks();
          if (src.chars().allMatch(Character::isWhitespace)) {
            // TODO make ignoring whitespace optional...
            // probably by enforcing a supertype on the Ctx argument...
            return;
          }
          printer.print(".append(\"");
          printer.print(X_Source.escape(src));
          printer.print("\")");
        } else {
          uiExpr.accept(this, arg);
        }
      });
    }

    public void printTo(Printable printer, Ctx arg) {
      this.printer = printer;
      expr.accept(this, arg);
    }

  }

  @Override
  public String transformUi(Printable printer, UiExpr ui) {
    ElementalUiVisitor visitor = new ElementalUiVisitor(ui);
    visitor.printTo(printer, getArgument());
    return Transformer.DO_NOT_PRINT;
  }

  protected Ctx getArgument() {
    return null;
  }
}
