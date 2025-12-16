package net.wti.lang.parser.ast.expr;

import net.wti.lang.parser.ast.visitor.GenericVisitor;
import net.wti.lang.parser.ast.visitor.VoidVisitor;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public class CssValueExpr extends CssExpr {

  private Expression value;
  private String unit;
  private boolean important;

  public CssValueExpr(
      Expression value
  ) {
    this(value.getBeginLine(), value.getBeginColumn(), value.getEndLine(), value.getEndColumn(),
          value, null, false);
  }

  public CssValueExpr(
      Expression value,
      String unit,
      boolean important
  ) {
    this(value.getBeginLine(), value.getBeginColumn(), value.getEndLine(), value.getEndColumn(),
          value, unit, important);
  }

  public CssValueExpr(
      final int beginLine,
      final int beginColumn,
      final int endLine,
      final int endColumn,
      Expression value,
      String unit,
      boolean important
  ) {
    super(beginLine, beginColumn, endLine, endColumn);
    this.value = value;
    this.unit = unit;
    this.important = important;
  }

  public Expression getValue() {
    return value;
  }

  public String getUnit() {
    return unit;
  }

  public boolean isImportant() {
    return important;
  }

  public void setValue(Expression value) {
    this.value = value;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  public void setImportant(boolean important) {
    this.important = important;
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
