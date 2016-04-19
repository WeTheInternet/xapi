package com.github.javaparser.ast.expr;

import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;

import java.util.List;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public class UiContainerExpr extends UiExpr {

  private boolean inTemplate;
  private NameExpr name;
  private UiBodyExpr body;
  private List<UiAttrExpr> attributes;

  public UiContainerExpr(final int beginLine, final int beginColumn, final int endLine, final int endColumn,
                         NameExpr name, List<UiAttrExpr> attributes, UiBodyExpr body, boolean isInTemplate) {
    super(beginLine, beginColumn, endLine, endColumn);
    this.name = name;
    this.attributes = attributes;
    this.body = body;
    this.inTemplate = isInTemplate;
  }

  public List<UiAttrExpr> getAttributes() {
    return attributes;
  }

  public void setAttributes(List<UiAttrExpr> attributes) {
    this.attributes = attributes;
    setAsParentNodeOf(attributes);
  }

  public UiBodyExpr getBody() {
    return body;
  }

  public void setBody(UiBodyExpr body) {
    this.body = body;
  }

  @Override public <R, A> R accept(final GenericVisitor<R, A> v, final A arg) {
    return v.visit(this, arg);
  }

  @Override public <A> void accept(final VoidVisitor<A> v, final A arg) {
    v.visit(this, arg);
  }

  public String getName() {
    return name.getName();
  }

  public NameExpr getNameExpr() {
    assert name != null : "Null names are not allowed!";
    return name;
  }

  public void setNameExpr(NameExpr name) {
      assert name != null : "Null names are not allowed!";
      this.name = name;
  }

  public void setName(String name) {
      assert name != null : "Null names are not allowed!";
      this.name = new NameExpr(name);
  }

  public boolean isInTemplate() {
    return inTemplate;
  }

  public void setInTemplate(boolean inTemplate) {
    this.inTemplate = inTemplate;
  }
}
