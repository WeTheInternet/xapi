package xapi.ui.api;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;
import xapi.fu.In1Out1;
import xapi.fu.In2;

/**
 * @author James X. Nelson (james@wetheinter.net)
 *         Created on 4/19/16.
 */
public class UiWithProperties <Node, E extends UiElement<Node, ? extends Node, E>> implements UiFeature {

  private In1Out1<String, Object> getter;
  private In2<String, Object> setter;

  public UiWithProperties() {
    final StringTo<Object> values = X_Collect.newStringMap(Object.class);
    getter = values::get;
    setter = values::put;
  }

  public UiWithProperties(E element) {
    getter = findGetter(element);
    if (getter != null) {
      setter = findSetter(element);
    }
    if (setter == null) {
      final StringTo<Object> values = X_Collect.newStringMap(Object.class);
      getter = values::get;
      setter = values::put;
    }
  }

  protected In2<String, Object> findSetter(E element) {
    return null;
  }

  protected In1Out1<String, Object> findGetter(E element) {
    return null;
  }

  public Object getProperty(String key) {
    return getter.io(key);
  }

  public void setProperty(String key, Object value) {
    setter.in(key, value);
  }
}
