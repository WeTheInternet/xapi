package xapi.ui.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.IntTo;
import xapi.except.NotImplemented;
import xapi.fu.Lazy;
import xapi.ui.api.UiElement;
import xapi.ui.api.UiFeature;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/19/16.
 */
public abstract class
    AbstractUiElement <Node, Element extends Node, Self extends UiElement<Node, ? extends Node, Self>>
    implements UiElement<Node, Element,Self> {

  protected Self parent;
  protected final IntTo<Self> children;
  protected final ClassTo<UiFeature> features;
  protected Lazy<Element> element;

  public <S extends Self> AbstractUiElement(Class<S> cls) {
    children = X_Collect.newList(cls);
    features = X_Collect.newClassMap(UiFeature.class);
    element = Lazy.deferred1(this::initAndBind);
  }

  protected final Element initAndBind() {
    Element e = initialize();
    getUiService().bindNode(e, ui());
    return e;
  }
  public Element initialize() {
    throw new NotImplemented("Class " + getClass() + " must implement initialize()");
  }

  @Override
  public Element element() {
    return element.out1();
  }

  @Override
  public String toSource() {
    throw new NotImplemented(getClass() + " must implement toSource()");
  }

  public void setElement(Element element) {
    assert this.element.isUnresolved() : "Calling setElement after element has already been resolved";
    this.element = Lazy.immutable1(element);
    final Self was = getUiService().bindNode(element, ui());
    assert was == null || was == this : "Binding native node " + element + " to multiple elements;" +
        "\nwas: " + was + "," +
        "\nis: " + this;
  }

  @Override
  public Self getParent() {
    return parent;
  }

  @Override
  public Self setParent(Self parent) {
    this.parent = parent;
    return ui();
  }


  public IntTo<Self> getChildren() {
    return children;
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
    return element() == that.element();
  }

  @Override
  public int hashCode() {
    return element().hashCode();
  }
}
