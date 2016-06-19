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
    <Element, Self extends UiElement<? extends Element, Self>>
    extends ElementInjector<Element, Self> {

  UiElement<? extends Element, ?> getParent();

  <E extends UiElement<? extends Element, E>> IntTo<E> getChildren();

  String toSource();

  Element element();

  default <El extends Self> Self getHost() {
    return (El) UiService.getUiService().getHost(this);
  }

  <F extends UiFeature, Generic extends F> F getFeature(Class<Generic> cls);

  <F extends UiFeature, Generic extends F> F addFeature(Class<Generic> cls, F feature);

  default <F extends UiFeature, Generic extends F> F getOrAddFeature(Class<Generic> cls, In1Out1<Self, F> factory) {
    F f = getFeature(cls);
    if (f == null) {
      f = factory.io(self());
      // purposely not null checking.  Will leave this to implementors to enforce.
      addFeature(cls, f);
    }
    return f;
  }

  default Self self() {
    return (Self) this;
  }

  default <F extends UiFeature, Generic extends F> F createFeature(Class<Generic> cls) {
    return X_Inject.instance(cls);
  }

  default UiWithAttributes getAttributes() {
    return getOrAddFeature(UiWithAttributes.class, e->getUiService().newAttributes(e));
  }

  default UiWithProperties getProperties() {
    return getOrAddFeature(UiWithProperties.class, e->getUiService().newProperties(e));
  }

  default UiService getUiService() {
    return UiService.getUiService();
  }

  @Override
  default <El extends Self> void appendChild(El newChild) {
    asInjector().appendChild(newChild);
  }

  @Override
  default <El extends Self> void removeChild(El child) {
    asInjector().removeChild(child);
  }

  <El extends Self> void insertAdjacent(ElementPosition pos, El child);

  default <El extends Self> void insertBefore(El newChild) {
    asInjector().insertBefore(newChild);
  }

  default <El extends Self> void insertAtBegin(El newChild) {
    asInjector().insertAtBegin(newChild);
  }

  default <El extends Self> void insertAfter(El newChild) {
    asInjector().insertAfter(newChild);
  }

  default <El extends Self> void insertAtEnd(El newChild) {
    asInjector().insertAtEnd(newChild);
  }

  default ElementInjector<Element, Self> asInjector() {
    // Platforms like Gwt might erase the type information off a
    // raw html / javascript type, so we return "real java objects" here.
    // This also allows implementors to insert control logic to the element attachment methoods.
    return new DelegateElementInjector(this);
  }

  default boolean removeFromParent() {
    final UiElement parent = getParent();
    if (parent != null) {
      parent.removeChild(this);
      assert getParent() == null : "Parent did not correctly detach child." +
          "\nChild " + toSource()+
          "\nParent " + parent.toSource();
      return true;
    }
    return false;
  }
}
