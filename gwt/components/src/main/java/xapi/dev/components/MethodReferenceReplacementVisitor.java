package xapi.dev.components;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.TemplateLiteralExpr;
import com.github.javaparser.ast.expr.UiAttrExpr;
import com.github.javaparser.ast.expr.UiContainerExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import xapi.dev.ui.GeneratedComponentMetadata;
import xapi.log.X_Log;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 5/29/16.
 */
public class MethodReferenceReplacementVisitor extends VoidVisitorAdapter<GeneratedComponentMetadata> {

  protected static final AtomicInteger random = new AtomicInteger();

  public static MethodReferenceReplacementVisitor mutateExpression(UiContainerExpr container, GeneratedComponentMetadata metadata) {
    MethodReferenceReplacementVisitor visitor = new MethodReferenceReplacementVisitor();
    container.accept(visitor, metadata);
    return visitor;
  }

  private UiContainerExpr container;
  private UiAttrExpr attr;

  @Override
  public void visit(
      UiContainerExpr n, GeneratedComponentMetadata arg
  ) {
    final UiContainerExpr oldContainer = container;
    container = n;
    super.visit(n, arg);
    container = oldContainer;
  }

  @Override
  public void visit(
      UiAttrExpr n, GeneratedComponentMetadata arg
  ) {
    final UiAttrExpr oldAttr = attr;
    attr = n;
    super.visit(n, arg);
    attr = oldAttr;
  }

  @Override
  public void visit(
      MethodReferenceExpr n, GeneratedComponentMetadata arg
  ) {
    container.removeAttribute(attr);
    final Optional<UiAttrExpr> idAttr = container.getAttribute("id");
    String ident;
    if (idAttr.isPresent()) {
      ident = extractAttrValue(idAttr.get());
    } else {
      ident = "gen"+Integer.toString(random.incrementAndGet(), 32);
      container.addAttribute(true, new UiAttrExpr(
          new NameExpr("id"), false, new StringLiteralExpr(ident)
      ));
    }
    // Now, knowing the ident, we can add a modifier to find and wire up our reference at runtime.
    // for now, we're going to hack in only event handlers...
    String attrName = attr.getNameString();
    if (attrName.startsWith("on")) {
      attrName = attrName.substring(2);
    }

    String elementType = arg.getElementTypeImported();
    String name = attrName.toLowerCase();
    String scopedName = extractStringValue(n.getScope());
    boolean isThis = scopedName.equals("this");
    boolean is$This = scopedName.equals("$this");
    if (is$This) {
      arg.ensure$this();
    }
    arg.addModifier(ele ->
      elementType + " " + ident + " = " +
          ele + ".querySelector(\"#" + ident + "\");"
    );
    arg.addModifier(ele ->
      ident + ".addEventListener(\"" + name + "\","
    );
    if (is$This) {
      arg.addModifier(ele -> n.toSource());
    } else {
      arg.addModifier(ele ->
        "todo->{}"
      );
    }
    arg.addModifier(ele ->
      ");"
    );
    super.visit(n, arg);
  }

  private String extractAttrValue(UiAttrExpr attr) {
    final Expression value = attr.getExpression();
    return extractStringValue(value);
  }

  private String extractStringValue(Expression value) {
    if (value instanceof StringLiteralExpr) {
      return ((StringLiteralExpr)value).getValue();
    } else if (value instanceof TemplateLiteralExpr) {
      return ((TemplateLiteralExpr)value).getValue();
    } else if (value instanceof NameExpr) {
      return ((NameExpr)value).getName();
    } else {
      String error = "Unhandled attribute value; cannot extract compile time string literal from " + value+" \nSource: " + attr;
      X_Log.error(getClass(), error);
      throw new IllegalArgumentException(error);
    }
  }
}
