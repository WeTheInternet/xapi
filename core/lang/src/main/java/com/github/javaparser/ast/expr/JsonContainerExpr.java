package com.github.javaparser.ast.expr;

import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

import java.util.List;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public class JsonContainerExpr extends JsonExpr {

  private final List<JsonPairExpr> pairs;
  private final boolean isArray;

  public JsonContainerExpr(
      final int beginLine,
      final int beginColumn,
      final int endLine,
      final int endColumn,
      boolean isArray,
      List<JsonPairExpr> pairs
  ) {
    super(beginLine, beginColumn, endLine, endColumn);
    this.pairs = pairs;
    this.isArray = isArray;
  }

  public boolean isArray() {
    return isArray;
  }

  public List<JsonPairExpr> getPairs() {
    return pairs;
  }

  @Override
  public <R, A> R accept(GenericVisitor<R, A> v, A arg) {
    for (JsonPairExpr pair : pairs) {
      R r = pair.accept(v, arg);
      if (r != null) {
        return r;
      }
    }
    return null;
  }

  @Override
  public <A> void accept(VoidVisitor<A> v, A arg) {
    for (JsonPairExpr pair : pairs) {
      pair.accept(v, arg);
    }
  }
}
