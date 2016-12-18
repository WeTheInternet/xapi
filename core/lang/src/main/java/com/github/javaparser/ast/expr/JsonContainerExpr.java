package com.github.javaparser.ast.expr;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.exception.NotFoundException;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import xapi.collect.X_Collect;
import xapi.fu.MappedIterable;
import xapi.fu.has.HasSize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public class JsonContainerExpr extends JsonExpr implements HasSize {

  private List<JsonPairExpr> pairs;
  private boolean isArray;

  public JsonContainerExpr(
      Iterable<Expression> exprs
  ) {
    this(-1, -1, -1, -1, true, exprToPairs(exprs));
  }

  private static List<JsonPairExpr> exprToPairs(Iterable<Expression> exprs) {
    final Iterator<Expression> itr = exprs.iterator();
    final List<JsonPairExpr> result = new ArrayList<>();
    for (int i = 0; itr.hasNext(); i++) {
      result.add(new JsonPairExpr(Integer.toString(i), itr.next()));
    }
    return result;
  }

  public JsonContainerExpr(
      boolean isArray,
      List<JsonPairExpr> pairs
  ) {
    this(-1, -1, -1, -1, isArray, pairs);
  }

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

  public MappedIterable<Expression> getValues() {
    return MappedIterable.mapped(pairs).map(JsonPairExpr::getValueExpr);
  }

  public MappedIterable<Expression> getKeys() {
    return MappedIterable.mapped(pairs).map(JsonPairExpr::getKeyExpr);
  }

  public void setPairs(List<JsonPairExpr> pairs) {
    this.pairs = pairs;
    setAsParentNodeOf(pairs);
  }

  public boolean hasNode(String name) {
    for (JsonPairExpr pair : getPairs()) {
      if (name.equals(ASTHelper.extractStringValue(pair.getKeyExpr()))) {
        return true;
      }
    }
    return false;
  }

  public Expression getNode(String name) {
    for (JsonPairExpr pair : getPairs()) {
      if (name.equals(ASTHelper.extractStringValue(pair.getKeyExpr()))) {
        return pair.getValueExpr();
      }
    }
    throw new NotFoundException(name);
  }

  public Expression getNode(int index) {
    if (index >= pairs.size() || index < 0) {
      throw new IndexOutOfBoundsException(index + " not within bounds of " + pairs.size());
    }
    final JsonPairExpr pair = pairs.get(index);
    assert Integer.toString(index).equals(ASTHelper.extractStringValue(pair.getKeyExpr()));
    return pair.getValueExpr();
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

  public static <N extends Expression> JsonContainerExpr jsonArray(N ... nodes) {
    return jsonArray(X_Collect.arrayIterable(nodes));
  }

  public static JsonContainerExpr jsonObject(Expression ... nodes) {
    assert nodes.length % 2 == 1 : "Json objects must have an even multiple of nodes; you sent " + nodes.length
        +"\n:" + Arrays.asList(nodes);
    List<JsonPairExpr> pairs = new ArrayList<>();
    for (int i = 0, m = nodes.length; i < m; i+=2) {
      pairs.add(new JsonPairExpr(nodes[i], nodes[i+1]));
    }
    return new JsonContainerExpr(false, pairs);
  }

  public static <N extends Expression> JsonContainerExpr jsonArray(Iterable<N> nodes) {
    List<JsonPairExpr> pairs = new ArrayList<>();
    int cnt = 0;
    for (N node : nodes) {
      pairs.add(new JsonPairExpr(Integer.toString(cnt++), node));
    }
    return new JsonContainerExpr(true, pairs);
  }

  @Override
  public int size() {
    return pairs.size();
  }
}
