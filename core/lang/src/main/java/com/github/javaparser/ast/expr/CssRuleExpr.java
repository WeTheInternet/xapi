package com.github.javaparser.ast.expr;

import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public class CssRuleExpr extends CssExpr {

  private final Expression key;
  private final Expression value;

  public CssRuleExpr(
      final int beginLine,
      final int beginColumn,
      final int endLine,
      final int endColumn,
      Expression key,
      Expression value
  ) {
    super(beginLine, beginColumn, endLine, endColumn);
    this.key = key;
    this.value = value;
  }

  public Expression getKey() {
    return key;
  }

  public Expression getValue() {
    return value;
  }

  @Override
  public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
    R r = key.accept(v, arg);
    if (r != null) {
      return r;
    }
    r = value.accept(v, arg);
    return r;
  }

  @Override
  public <A> void accept(VoidVisitor<A> v, A arg) {
    key.accept(v, arg);
    value.accept(v, arg);
  }
}
