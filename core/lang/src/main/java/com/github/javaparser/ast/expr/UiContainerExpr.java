package com.github.javaparser.ast.expr;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.visitor.GenericVisitor;
import com.github.javaparser.ast.visitor.VoidVisitor;
import xapi.fu.Filter.Filter1;
import xapi.fu.In1Out1;
import xapi.fu.Maybe;
import xapi.fu.data.ListLike;
import xapi.fu.data.MultiList;
import xapi.fu.itr.MappedIterable;
import xapi.fu.java.X_Jdk;

import java.util.ArrayList;
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
  private MultiList<String, UiAttrExpr> attrs;
  private String docType;

  public UiContainerExpr(String name) {
    this(-1, -1, -1, -1, new NameExpr(name), new ArrayList<>(), null, false);
  }

  public UiContainerExpr(final int beginLine, final int beginColumn, final int endLine, final int endColumn,
                         NameExpr name, List<UiAttrExpr> attributes, UiBodyExpr body, boolean isInTemplate) {
    super(beginLine, beginColumn, endLine, endColumn);
    attrs = X_Jdk.multiList();
    this.name = name;
    this.body = body;
    this.inTemplate = isInTemplate;
    setAsParentNodeOf(body);
    setAttributes(attributes);
  }

  public List<UiAttrExpr> getAttributes() {
    return X_Jdk.asList(attrs.flatten());
  }

  public MappedIterable<UiAttrExpr> attrs() {
    return attrs.mappedValues().flatten(ListLike::forEachItem);
  }

  public UiAttrExpr addAttribute(String name, Expression value) {
    final UiAttrExpr attr = UiAttrExpr.of(name, value);
    addAttribute(false, attr);
    return attr;
  }

  public UiAttrExpr addAttribute(boolean replace, UiAttrExpr attr) {
    final List<Node> children = getChildrenNodes();
    final ListLike<UiAttrExpr> into = attrs.get(attr.getNameString());
    if (replace) {
      into.forAll(children::remove);
      into.clear();
    }
    into.add(attr);
    children.add(attr);
    setAsParentNodeOf(attr);
    return attr;
  }

  public ListLike<UiAttrExpr> getAttributes(String name) {
    return attrs.get(name);
  }

  public UiAttrExpr getAttributeNotNull(String name) {
    return getAttribute(name).getOrThrow(()->new NullPointerException("No feature named " + name + " found in " + this));
  }
  public MappedIterable<UiAttrExpr> getAttributesMatching(Filter1<UiAttrExpr> filter) {
    return attrs.flatten()
            .filter(filter);
  }

  public Maybe<Expression> attrExpr(String name) {
    return getAttribute(name)
        .mapNullSafe(UiAttrExpr::getExpression);

  }

  public Maybe<UiAttrExpr> getAttribute(String name) {
    final ListLike<UiAttrExpr> avail = attrs.get(name);
    assert avail.size() < 2 : "Asked for a single attribute, but value of " + name +" had more than 1 item: " + avail;
    return avail.isEmpty() ? Maybe.not() : Maybe.nullable(avail.get(0));
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
    final ListLike<UiAttrExpr> list = attrs.get(attr.getNameString());
    getChildrenNodes().remove(attr);
    if (-1 != list.removeFirst(attr, false)) {
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

  public UiContainerExpr getContainerParentNode() {
    Node next = this.getParentNode();
    while (next != null) {
      if (next instanceof UiContainerExpr) {
        return (UiContainerExpr) next;
      }
      next = next.getParentNode();
    }
    return null;
  }

  public String getDocType() {
    return docType;
  }

  public void setDocType(String docType) {
    this.docType = docType;
  }

  public String getAttributeRequiredString(String id) {
    final Expression expr = getAttributeNotNull(id).getExpression();
    if (expr instanceof StringLiteralExpr) {
      return ((StringLiteralExpr) expr).getValue();
    }
    if (expr instanceof TemplateLiteralExpr) {
      return ((TemplateLiteralExpr) expr).getValueWithoutTicks();
    }
    return expr.toSource();
  }
}
