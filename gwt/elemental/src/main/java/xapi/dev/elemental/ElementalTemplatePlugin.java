package xapi.dev.elemental;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.plugin.Transformer;
import com.github.javaparser.ast.plugin.UiTranslatorPlugin;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import xapi.fu.Printable;
import xapi.log.X_Log;
import xapi.source.X_Source;
import xapi.util.api.DebugRethrowable;

import java.util.List;
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
    private boolean inCssBlock;

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
    public void visit(CssBlockExpr n, Ctx arg) {
      inCssBlock = true;
      if (n.getContainers().size() == 1 && n.getContainers().get(0).getSelectors().isEmpty()) {
        // This is an inline .{ rule: val; } which does not have any selectors.
        // In this case, just print the rules...
        printer.print("\"");
        n.getContainers().get(0).getRules().forEach(rule->rule.accept(this, arg));
        printer.print("\"");
      } else {
        // A more complex block container which has selectors and rules...
        // will need to create css files and inject them
        printer.print("null");
        super.visit(n, arg);
      }
      inCssBlock = false;
    }

    @Override
    public void visit(CssContainerExpr n, Ctx arg) {
      if (!inCssBlock && n.isSingleClassSelector()) {
        // This is a single class selector outside of a css block;
        // in this case, we want to print the style externally,
        // then print the single classname as a string.
        printer.print("\"")
               .print(n.getSelectors().get(0).getParts().get(0).substring(1))
               .print("\"");
      }
      // Any other css container should be printed as a css file,
      // possibly with a factory that can call any expressions that
      // happen to be included in said css.
      printer.println(")")
             .print(".addCss(")
             .indent()
             .println()
             .print("\"");
      final List<CssSelectorExpr> selectors = n.getSelectors();
      for (int i = 0; i < selectors.size(); i++) {
        selectors.get(i).accept(this, arg);
        if (i < selectors.size() -1) {
          printer.println(", \"")
                 .print("\"");

        }
      }
      printer.println(" {\" +");
      printer.indent();
      final List<CssRuleExpr> rules = n.getRules();
      for (int i = 0; i < rules.size(); i++) {
        printer.print("\"");
        rules.get(i).accept(this, arg);
        if (i == rules.size() - 1) {
          printer.outdent();
        }
        printer.println("\" +");
      }
      printer.println("\"}\"");
      printer.outdent();
    }

    @Override
    public void visit(CssSelectorExpr n, Ctx arg) {
      final List<String> parts = n.getParts();
      for (int i = 0; i < parts.size(); i++) {
        String part = parts.get(i);
        printer.print(part);
        if (i < parts.size() - 1) {
          String next = parts.get(i + 1);
          if (!next.startsWith(":") || next.startsWith("::selection")) {
            printer.print(" ");
          }
        }
      }
    }

    @Override
    public void visit(CssRuleExpr n, Ctx arg) {
      // TODO: Handle parenthetical key expressions correctly
      String key = ASTHelper.extractStringValue(n.getKey());
      printer.print(key).print(" : ");
      n.getValue().accept(this, arg);
      printer.print(";");
    }

    @Override
    public void visit(CssValueExpr n, Ctx arg) {
      n.getValue().accept(this, arg);
      if (n.getUnit() != null) {
        printer.print(n.getUnit());
      }
    }

    @Override
    public void visit(IntegerLiteralExpr n, Ctx arg) {
      printer.print(n.getValue());
    }

    @Override
    public void visit(StringLiteralExpr n, Ctx arg) {
      printer.print(n.getValue());
    }

    @Override
    public void visit(DoubleLiteralExpr n, Ctx arg) {
      printer.print(n.getValue());
    }

    @Override
    public void visit(LongLiteralExpr n, Ctx arg) {
      printer.print(n.getValue());
    }

    @Override
    public void visit(CharLiteralExpr n, Ctx arg) {
      printer.print(n.getValue());
    }

    @Override
    public void visit(UiAttrExpr n, Ctx arg) {
      printer.indent();
      printer.println();
      printer.print(".set(\"");
      printer.print(n.getName().getName());
      printer.print("\", ");
      final Expression value = n.getExpression();
      boolean keepTrying = true;
      if (value instanceof LiteralExpr) {
        keepTrying = false;
        if (value instanceof CssExpr || value instanceof UiContainerExpr || value instanceof JsonExpr) {
          value.accept(this, arg);
        } else {
          String val;
          try {
            val = ASTHelper.extractStringValue(value);
            if (value.getClass() == StringLiteralExpr.class ||
                  value instanceof TemplateLiteralExpr ||
                  value instanceof UiExpr) {
              val = "\"" + X_Source.escape(val) + "\"";
            }
            printer.print(val);
          } catch (Exception ignored) {
            keepTrying = true;
            X_Log.debug(getClass(), "ignored exception: ", ignored);
          }
        }
      }
      if (keepTrying) {
        if (value instanceof NameExpr){
          String name = value.toSource();
          if (name.startsWith("$")) {
            // A $dollar reference!
            // We'll want to emit a magic method to handle type coercions in compiler...
            printer.print("xapi.fu.X_Fu.coerce(" + name.substring(1) +")");
          }
        } else {
          value.accept(this, arg);
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
