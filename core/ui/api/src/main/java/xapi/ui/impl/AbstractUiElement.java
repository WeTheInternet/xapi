package xapi.ui.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.ClassTo;
import xapi.collect.api.IntTo;
import xapi.ui.api.UiElement;
import xapi.ui.api.UiFeature;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/19/16.
 */
public abstract class AbstractUiElement <Self extends AbstractUiElement<Self>> implements UiElement {

  protected UiElement parent;
  protected final IntTo<UiElement> children;
  protected final ClassTo<UiFeature> features;

  public AbstractUiElement() {
    children = X_Collect.newList(UiElement.class);
    features = X_Collect.newClassMap(UiFeature.class);
  }

  @Override
  public UiElement getParent() {
    return parent;
  }

  @Override
  public IntTo<UiElement> getChildren() {
    return children;
  }

  @Override
  public void appendChild(UiElement child) {
    children.add(child);
  }

  @Override
  public void removeChild(UiElement child) {
    children.remove(child);
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

}
