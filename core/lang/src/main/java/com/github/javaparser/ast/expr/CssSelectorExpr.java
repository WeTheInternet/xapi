package com.github.javaparser.ast.expr;

import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

import java.util.List;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public class CssSelectorExpr extends CssExpr {

  private final List<String> parts;

  public CssSelectorExpr(
      final int beginLine,
      final int beginColumn,
      final int endLine,
      final int endColumn,
      List<String> parts
  ) {
    super(beginLine, beginColumn, endLine, endColumn);
    this.parts = parts;
  }

  public List<String> getParts() {
    return parts;
  }

  @Override
  public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
    return null;
  }

  @Override
  public <A> void accept(VoidVisitor<A> v, A arg) {
    v.visit(this, arg);
  }
}
