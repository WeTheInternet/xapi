package xapi.collect.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;

public class StringToDeepMultiMap<X> extends StringToAbstract<StringTo<IntTo<X>>>{
  private static final long serialVersionUID = 3345558328628203700L;
  private final Class<? extends X> componentClass;

  public StringToDeepMultiMap(final Class<? extends X> componentClass) {
    super(Class.class.cast(StringTo.class));
    this.componentClass = componentClass;
  }

  @Override
  public final StringTo<IntTo<X>> get(final String key) {
    StringTo<IntTo<X>> list = super.get(key);
    if (list == null) {
      list = newMap();
      put(key, list);
    }
    return list;
  }

  @SuppressWarnings("unchecked")
  protected StringTo<IntTo<X>> newMap() {
    return X_Collect.newStringMultiMap(Class.class.cast(componentClass));
  }

}
