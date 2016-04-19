package com.github.javaparser.ast.expr;

import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

import java.util.List;

/**
 * A
 *
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public class UiBodyExpr extends UiExpr {

  private List<UiExpr> children;

  public UiBodyExpr(
      final int beginLine, final int beginColumn, final int endLine, final int endColumn,
      List<UiExpr> children) {
    super(beginLine, beginColumn, endLine, endColumn);
    this.children = children;
  }

  @Override public <R, A> R accept(final GenericVisitor<R, A> v, final A arg) {
    return v.visit(this, arg);
  }

  @Override public <A> void accept(final VoidVisitor<A> v, final A arg) {
    v.visit(this, arg);
  }

  public List<UiExpr> getChildren() {
    return children;
  }

  public void setChildren(List<UiExpr> children) {
    this.children = children;
    setAsParentNodeOf(children);
  }

  public boolean isNotEmpty() {
    return !children.isEmpty();
  }

  public boolean isEmpty() {
    return children.isEmpty();
  }
}
