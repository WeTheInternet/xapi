package net.wti.lang.parser.ast.expr;

import net.wti.lang.parser.ast.visitor.GenericVisitor;
import net.wti.lang.parser.ast.visitor.VoidVisitor;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public class CssRuleExpr extends CssExpr {

  private Expression key;
  private Expression value;

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

  public void setKey(Expression key) {
    this.key = key;
  }

  public void setValue(Expression value) {
    this.value = value;
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
