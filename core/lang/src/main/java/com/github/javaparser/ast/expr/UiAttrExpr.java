package com.github.javaparser.ast.expr;

import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public class UiAttrExpr extends UiExpr {

  private NameExpr name;
  private boolean attribute;
  private Expression expression;

  public UiAttrExpr(
      final int beginLine,
      final int beginColumn,
      final int endLine,
      final int endColumn,
      NameExpr name,
      boolean attribute,
      Expression expression
  ) {
    super(beginLine, beginColumn, endLine, endColumn);
    this.name = name;
    this.attribute = attribute;
    this.expression = expression;
  }

  public UiAttrExpr(
      NameExpr name,
      boolean attribute,
      Expression expression
  ) {
    super();
    this.name = name;
    this.attribute = attribute;
    this.expression = expression;
  }

  public NameExpr getName() {
    return name;
  }

  public void setName(NameExpr name) {
    this.name = name;
  }

  public Expression getExpression() {
    return expression;
  }

  public void setExpression(Expression expression) {
    this.expression = expression;
  }

  public boolean isAttribute() {
    return attribute;
  }

  public void setAttribute(boolean attribute) {
    this.attribute = attribute;
  }

  @Override public <R, A> R accept(final GenericVisitor<R, A> v, final A arg) {
    return v.visit(this, arg);
  }

  @Override public <A> void accept(final VoidVisitor<A> v, final A arg) {
    v.visit(this, arg);
  }

}
