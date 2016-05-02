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
public interface UiElement extends ElementInjector {

  UiElement getParent();

  IntTo<UiElement> getChildren();

  void appendChild(UiElement child);

  String toSource();

  <F extends UiFeature, Generic extends F> F getFeature(Class<Generic> cls);

  <F extends UiFeature, Generic extends F> F addFeature(Class<Generic> cls, F feature);

  default <F extends UiFeature, Generic extends F> F getOrAddFeature(Class<Generic> cls, In1Out1<UiElement, F> factory) {
    F f = getFeature(cls);
    if (f == null) {
      f = factory.io(this);
      // purposely not null checking.  Will leave this to implementors to enforce.
      addFeature(cls, f);
    }
    return f;
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

  void insertAdjacent(ElementPosition newChild, UiElement beforeChild);

  default void insertBefore(UiElement newChild) {
    asInjector().insertBefore(newChild);
  }

  default void insertAtBegin(UiElement newChild) {
    asInjector().insertAtBegin(newChild);
  }

  default void insertAfter(UiElement newChild) {
    asInjector().insertAfter(newChild);
  }

  default void insertAtEnd(UiElement newChild) {
    asInjector().insertAtEnd(newChild);
  }

  default ElementInjector asInjector() {
    // Platforms like Gwt might erase the type information off a
    // raw html / javascript type, so we return "real java objects" here.
    // This also allows implementors
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
