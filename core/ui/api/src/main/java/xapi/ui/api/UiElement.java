package xapi.ui.api;

import xapi.collect.api.IntTo;
import xapi.fu.In1Out1;
import xapi.inject.X_Inject;
import xapi.ui.impl.DelegateElementInjector;
import xapi.ui.service.UiService;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/18/16.
 */
public interface UiElement
    <Node, Element extends Node, Base extends UiElement<Node, ? extends Node, Base>>
    extends ElementInjector<Node, Base> {

  Base getParent();

  Base setParent(Base parent);

  IntTo<Base> getChildren();

  String toSource();

  Element element();

  default Node getHost() {
    return (Node)UiService.getUiService().getHost(this);
  }

  <F extends UiFeature, Generic extends F> F getFeature(Class<Generic> cls);

  <F extends UiFeature, Generic extends F> F addFeature(Class<Generic> cls, F feature);

  default <F extends UiFeature, Generic extends F> F getOrAddFeature(Class<Generic> cls, In1Out1<Base, F> factory) {
    F f = getFeature(cls);
    if (f == null) {
      f = factory.io(self());
      // purposely not null checking.  Will leave this to implementors to enforce.
      addFeature(cls, f);
    }
    return f;
  }

  default <F extends UiFeature, Generic extends F> F createFeature(Class<Generic> cls) {
    return X_Inject.instance(cls);
  }

  @SuppressWarnings("unchecked")
  default Base self() {
    return (Base) this;
  }

  default UiWithAttributes<Node, Base> getAttributes() {
    return getOrAddFeature(UiWithAttributes.class, e->getUiService().newAttributes(e));
  }

  default UiWithProperties<Node, Base> getProperties() {
    return getOrAddFeature(UiWithProperties.class, e->getUiService().newProperties(e));
  }

  default UiService<Node, Base> getUiService() {
    return UiService.getUiService();
  }

  @Override
  default void appendChild(Base newChild) {
    asInjector().appendChild(newChild);
    newChild.setParent(self());
  }

  @Override
  default void removeChild(Base child) {
    asInjector().removeChild(child);
  }

  void insertAdjacent(ElementPosition pos, Base child);

  default void insertBefore(Base newChild) {
    asInjector().insertBefore(newChild);
  }

  default void insertAtBegin(Base newChild) {
    asInjector().insertAtBegin(newChild);
  }

  default void insertAfter(Base newChild) {
    asInjector().insertAfter(newChild);
  }

  default void insertAtEnd(Base newChild) {
    asInjector().insertAtEnd(newChild);
  }

  default ElementInjector<Node, Base> asInjector() {
    // Platforms like Gwt might erase the type information off a
    // raw html / javascript type, so we return "real java objects" here.
    // This also allows implementors to insert control logic to the element attachment methods.
    return new DelegateElementInjector<>(self());
  }

  default boolean removeFromParent() {
    final Base parent = getParent();
    if (parent != null) {
      parent.removeChild(self());
      assert getParent() == null : "Parent did not correctly detach child." +
          "\nChild " + toSource()+
          "\nParent " + parent.toSource();
      return true;
    }
    return false;
  }
}
