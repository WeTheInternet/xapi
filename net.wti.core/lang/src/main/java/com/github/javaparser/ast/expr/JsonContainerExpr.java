package com.github.javaparser.ast.expr;

import com.github.javaparser.ASTHelper;
import com.github.javaparser.ast.exception.NotFoundException;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import xapi.fu.Maybe;
import xapi.fu.has.HasSize;
import xapi.fu.itr.MappedIterable;
import xapi.fu.itr.SingletonIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static xapi.fu.itr.ArrayIterable.iterate;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public class JsonContainerExpr extends JsonExpr implements HasSize {

  private List<JsonPairExpr> pairs;
  private boolean isArray;

  public JsonContainerExpr(Expression expr) {
    this(SingletonIterator.singleItem(expr));
  }

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
    return getNodeMaybe(name).getOrThrow(()->new NotFoundException(name));
  }
  public Maybe<Expression> getNodeMaybe(String name) {
    for (JsonPairExpr pair : getPairs()) {
      if (name.equals(ASTHelper.extractStringValue(pair.getKeyExpr()))) {
        return Maybe.immutable(pair.getValueExpr());
      }
    }
    return Maybe.not();
  }

  public Expression getNode(int index) {
    return getNodeMaybe(index).getOrThrow(()->new NotFoundException("No item at index "+index));
  }

  public Maybe<Expression> getNodeMaybe(int index) {
    if (index >= pairs.size() || index < 0) {
      throw new IndexOutOfBoundsException(index + " not within bounds of " + pairs.size());
    }
    final JsonPairExpr pair = pairs.get(index);
    if (pair == null) {
      return Maybe.not();
    }
    assert Integer.toString(index).equals(ASTHelper.extractStringValue(pair.getKeyExpr()));
    return Maybe.immutable(pair.getValueExpr());
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
    return jsonArray(iterate(nodes));
  }

  public static JsonContainerExpr jsonObject(Expression ... nodes) {
    assert nodes.length % 2 == 0 : "Json objects must have an even multiple of nodes; you sent " + nodes.length
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

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;

    final JsonContainerExpr that = (JsonContainerExpr) o;

    if (isArray != that.isArray)
      return false;
    return pairs.equals(that.pairs);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + pairs.hashCode();
    result = 31 * result + (isArray ? 1 : 0);
    return result;
  }

  public static boolean isArray(Expression expr) {
    return expr instanceof JsonContainerExpr && ((JsonContainerExpr)expr).isArray();
  }

  public static boolean isObject(Expression expr) {
    return expr instanceof JsonContainerExpr && !((JsonContainerExpr)expr).isArray();
  }
}
