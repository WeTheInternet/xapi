package com.github.javaparser.ast.expr;

import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

import java.util.List;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public class CssBlockExpr extends UiExpr {

  private final List<CssContainerExpr> containers;

  public CssBlockExpr(
      final int beginLine,
      final int beginColumn,
      final int endLine,
      final int endColumn,
      List<CssContainerExpr> containers
  ) {
    super(beginLine, beginColumn, endLine, endColumn);
    this.containers = containers;
  }

  public List<CssContainerExpr> getContainers() {
    return containers;
  }

  @Override
  public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
    for (CssContainerExpr container : containers) {
      R r = container.accept(v, arg);
      if (r != null) {
        return r;
      }
    }

    return null;
  }

  @Override
  public <A> void accept(VoidVisitor<A> v, A arg) {
    for (CssContainerExpr container : containers) {
      container.accept(v, arg);
    }

  }
}
