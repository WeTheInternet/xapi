package com.github.javaparser.ast.expr;

import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

import java.util.List;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public class CssBlockExpr extends UiExpr {

  private List<CssContainerExpr> containers;

  public CssBlockExpr(
      final int beginLine,
      final int beginColumn,
      final int endLine,
      final int endColumn,
      List<CssContainerExpr> containers
  ) {
    super(beginLine, beginColumn, endLine, endColumn);
    setContainers(containers);
  }

  public List<CssContainerExpr> getContainers() {
    return containers;
  }

  public void setContainers(List<CssContainerExpr> containers) {
    this.containers = containers;
    setAsParentNodeOf(containers);
  }

  @Override
  public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
    return v.visit(this, arg);
  }

  @Override
  public <A> void accept(VoidVisitor<A> v, A arg) {
    v.visit(this, arg);
  }
}
