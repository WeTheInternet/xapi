package com.github.javaparser.ast.expr;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import xapi.source.X_Source;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public class JsonPairExpr extends UiExpr {

  private Expression keyExpr;
  private Expression valueExpr;

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

  public void setKeyExpr(Expression keyExpr) {
    this.keyExpr = keyExpr;
    setAsParentNodeOf(keyExpr);
  }

  public void setValueExpr(Expression valueExpr) {
    this.valueExpr = valueExpr;
    setAsParentNodeOf(valueExpr);
  }

  @Override
  public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
    return v.visit(this, arg);
  }

  @Override
  public <A> void accept(VoidVisitor<A> v, A arg) {
    v.visit(this, arg);
  }

  public String getKeyString() {
    return ASTHelper.extractStringValue(getKeyExpr());
  }
  public String getKeyQuoted() {
    String asString = ASTHelper.extractStringValue(getKeyExpr());
    return asString.startsWith("\"") ? asString : "\"" + X_Source.escape(asString) + "\"";
  }
}
