package com.github.javaparser.ast.expr;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;
import xapi.fu.Filter.Filter1;
import xapi.fu.In1Out1;
import xapi.fu.MappedIterable;
import xapi.fu.Maybe;

import static xapi.collect.X_Collect.newStringMultiMap;

import java.util.List;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/10/16.
 */
public class UiContainerExpr extends UiExpr {

  private boolean inTemplate;
  private boolean alwaysRenderClose;
  private NameExpr name;
  private UiBodyExpr body;
  private StringTo.Many<UiAttrExpr> attrs;

  public UiContainerExpr(final int beginLine, final int beginColumn, final int endLine, final int endColumn,
                         NameExpr name, List<UiAttrExpr> attributes, UiBodyExpr body, boolean isInTemplate) {
    super(beginLine, beginColumn, endLine, endColumn);
    attrs = newStringMultiMap(UiAttrExpr.class);
    this.name = name;
    this.body = body;
    this.inTemplate = isInTemplate;
    setAsParentNodeOf(body);
    setAttributes(attributes);
  }

  public List<UiAttrExpr> getAttributes() {
    return attrs.flatten().asList();
  }

  public void addAttribute(boolean replace, UiAttrExpr attr) {
    final List<Node> children = getChildrenNodes();
    final IntTo<UiAttrExpr> into = attrs.get(attr.getNameString());
    if (replace) {
      into.forEachValue(children::remove);
      into.clear();
    }
    into.add(attr);
    children.add(attr);
    setAsParentNodeOf(attr);
  }

  public IntTo<UiAttrExpr> getAttributes(String name) {
    return attrs.get(name);
  }

  public UiAttrExpr getAttributeNotNull(String name) {
    return getAttribute(name).getOrThrow(()->new NullPointerException("No feature named " + name + " found in " + this));
  }
  public MappedIterable<UiAttrExpr> getAttributesMatching(Filter1<UiAttrExpr> filter) {
    return attrs.flattenedValues()
            .filter(filter);
  }
  public Maybe<UiAttrExpr> getAttribute(String name) {
    final IntTo<UiAttrExpr> avail = attrs.get(name);
    assert avail.size() < 2 : "Asked for a single attribute, but value of " + name +" had more than 1 item: " + avail;
    return avail.isEmpty() ? Maybe.not() : Maybe.nullable(avail.at(0));
  }

  public void setAttributes(List<UiAttrExpr> attributes) {
    final In1Out1<UiAttrExpr, String> mapper = UiAttrExpr::getNameString;
    attrs.clear();
    attrs.addManyMapped(attributes, mapper);
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

  public boolean removeAttribute(UiAttrExpr attr) {
    final IntTo<UiAttrExpr> list = attrs.get(attr.getNameString());
    getChildrenNodes().remove(attr);
    if (list.findRemove(attr, false)) {
      if (list.isEmpty()) {
        attrs.remove(attr.getNameString());
      }
      return true;
    }
    return false;
  }

  public void alwaysRenderClose() {
    this.alwaysRenderClose = true;
  }

  public boolean shouldRenderClose() {
    return alwaysRenderClose;
  }
}
