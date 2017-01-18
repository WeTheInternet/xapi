package com.github.javaparser.ast.expr;

import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import xapi.fu.Filter.Filter1;
import xapi.fu.Maybe;

import static com.github.javaparser.ast.expr.StringLiteralExpr.stringLiteral;

import java.util.List;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public class UiAttrExpr extends UiExpr {

  private NameExpr name;
  private boolean attribute;
  private Expression expression;
  // a synthetic attribute is anything created manually in java code
  // instead of being generated by the parser; using the line-numbered
  // constructor will result in synthetic=false; the easy ones are synthetic=true
  private boolean synthetic;
  private List<AnnotationExpr> annotations;

  public UiAttrExpr(
      final int beginLine,
      final int beginColumn,
      final int endLine,
      final int endColumn,
      NameExpr name,
      boolean attribute,
      Expression expression
  ) {
    super(beginLine, beginColumn, endLine, endColumn);
    this.name = name;
    this.attribute = attribute;
    this.expression = expression;
    setAsParentNodeOf(expression);
  }

  public UiAttrExpr(
      NameExpr name,
      boolean attribute,
      Expression expression
  ) {
    super();
    this.name = name;
    this.attribute = attribute;
    this.expression = expression;
    setAsParentNodeOf(expression);
    synthetic = true;
  }

  public UiAttrExpr(String name, Expression value) {
    this(new NameExpr(name), false, value);
  }

  public NameExpr getName() {
    return name;
  }

  public String getNameString() {
    return name.getName();
  }

  public void setName(NameExpr name) {
    this.name = name;
  }

  public Expression getExpression() {
    return expression;
  }

  public void setExpression(Expression expression) {
    this.expression = expression;
  }

  public boolean isAttribute() {
    return attribute;
  }

  public void setAttribute(boolean attribute) {
    this.attribute = attribute;
  }

  @Override public <R, A> R accept(final GenericVisitor<R, A> v, final A arg) {
    return v.visit(this, arg);
  }

  @Override public <A> void accept(final VoidVisitor<A> v, final A arg) {
    v.visit(this, arg);
  }

  public boolean isSynthetic() {
    return synthetic;
  }

  public List<AnnotationExpr> getAnnotations() {
    return annotations;
  }

  public void setAnnotations(List<AnnotationExpr> annotations) {
    this.annotations = annotations;
  }

  public Maybe<AnnotationExpr> getAnnotation(Filter1<AnnotationExpr> filter) {
    for (AnnotationExpr annotation : annotations) {
      if (filter.filter1(annotation)) {
        return Maybe.nullable(annotation);
      }
    }
    return Maybe.not();
  }

  public static UiAttrExpr of(String name, String stringValue) {
    return new UiAttrExpr(new NameExpr(name), name.startsWith("@"), stringLiteral(stringValue));
  }

  public static UiAttrExpr of(String name, Expression value) {
    return new UiAttrExpr(new NameExpr(name), name.startsWith("@"), value);
  }
}
