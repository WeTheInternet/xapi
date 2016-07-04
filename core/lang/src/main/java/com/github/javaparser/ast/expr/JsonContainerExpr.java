package com.github.javaparser.ast.expr;

import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

import java.util.List;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public class JsonContainerExpr extends JsonExpr {

  private List<JsonPairExpr> pairs;
  private boolean isArray;

  public JsonContainerExpr(
      final int beginLine,
      final int beginColumn,
      final int endLine,
      final int endColumn,
      boolean isArray,
      List<JsonPairExpr> pairs
  ) {
    super(beginLine, beginColumn, endLine, endColumn);
    setPairs(pairs);
    this.isArray = isArray;
  }

  public boolean isArray() {
    return isArray;
  }

  public List<JsonPairExpr> getPairs() {
    return pairs;
  }

  public void setPairs(List<JsonPairExpr> pairs) {
    this.pairs = pairs;
    setAsParentNodeOf(pairs);
  }

  public void setArray(boolean array) {
    isArray = array;
  }

  @Override
  public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
    return v.visit(this, arg);
  }

  @Override
  public <A> void accept(VoidVisitor<A> v, A arg) {
    v.visit(this, arg);
  }

  public boolean isEmpty() {
    return pairs.isEmpty();
  }
}
