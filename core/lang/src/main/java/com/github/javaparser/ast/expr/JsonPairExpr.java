package com.github.javaparser.ast.expr;

import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public class JsonPairExpr extends UiExpr {

  private final Expression keyExpr;
  private final Expression valueExpr;

  public JsonPairExpr(final int beginLine, final int beginColumn, final int endLine, final int endColumn, Expression keyExpr, Expression valueExpr) {
    super(beginLine, beginColumn, endLine, endColumn);
    this.keyExpr = keyExpr;
    this.valueExpr = valueExpr;
  }

  public Expression getKeyExpr() {
    return keyExpr;
  }

  public Expression getValueExpr() {
    return valueExpr;
  }

  @Override
  public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
    R r = getKeyExpr().accept(v, arg);
    if (r != null) {
      return r;
    }
    return getValueExpr().accept(v, arg);
  }

  @Override
  public <A> void accept(VoidVisitor<A> v, A arg) {
    getKeyExpr().accept(v, arg);
    getValueExpr().accept(v, arg);
  }
}
