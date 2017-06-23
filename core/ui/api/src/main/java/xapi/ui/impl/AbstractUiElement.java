package xapi.ui.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.IntTo;
import xapi.except.NotImplemented;
import xapi.fu.Lazy;
import xapi.fu.iterate.SizedIterable;
import xapi.ui.api.UiElement;
import xapi.ui.api.UiFeature;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/19/16.
 */
public abstract class
    AbstractUiElement <Node, Element extends Node, Base extends UiElement<Node, ? extends Node, Base>>
    implements UiElement<Node, Element, Base> {

  protected Base parent;
  protected final IntTo<Base> children;
  protected final ClassTo<UiFeature> features;
  protected Lazy<Element> element;

  public <S extends Base> AbstractUiElement(Class<S> cls) {
    children = X_Collect.newList(cls);
    features = X_Collect.newClassMap(UiFeature.class);
    element = Lazy.deferred1(this::initAndBind);
  }

  protected final Element initAndBind() {
    Element e = initialize();
    getUiService().bindNode(e, ui());
    return e;
  }
  protected Element initialize() {
    throw new NotImplemented("Class " + getClass() + " must implement initialize()");
  }

  @Override
  public Element getElement() {
    return element.out1();
  }

  @Override
  public String toSource() {
    throw new NotImplemented(getClass() + " must implement toSource()");
  }

  public void setElement(Element element) {
    assert this.element.isUnresolved() : "Calling setElement after element has already been resolved";
    this.element = Lazy.immutable1(element);
    final Base was = getUiService().bindNode(element, ui());
    assert was == null || was == this : "Binding native node " + element + " to multiple elements;" +
        "\nwas: " + was + "," +
        "\nis: " + this;
  }

  @Override
  public Base getParent() {
    return parent;
  }

  @Override
  public void setParent(Base parent) {
    this.parent = parent;
  }

  public SizedIterable<Base> getChildren() {
    return children.forEachItem().counted();
  }

  @Override
  public <F extends UiFeature, Generic extends F> F getFeature(Class<Generic> cls) {
    return (F) features.get(cls);
  }

  @Override
  public <F extends UiFeature, Generic extends F> F addFeature(Class<Generic> cls, F feature) {
    final UiFeature result = features.put(cls, feature);
    return (F) result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof UiElement))
      return false;

    final UiElement<?, ?, ?> that = (UiElement<?, ?, ?>) o;

    // Ui elements may use referential equality,
    // since two elements with the exact same attributes are not actually equal
    return getElement() == that.getElement();
  }

  @Override
  public int hashCode() {
    return getElement().hashCode();
  }
}
