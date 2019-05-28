package xapi.collect.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.StringTo;

public class StringToDeepMap <X> extends StringToAbstract<StringTo<X>>{
  private static final long serialVersionUID = 3345558328628203700L;
  private final Class<? extends X> componentClass;

  public StringToDeepMap(final Class<? extends X> componentClass) {
    super(Class.class.cast(StringTo.class));
    this.componentClass = componentClass;
  }

  @Override
  public StringTo<X> get(final String key) {
    StringTo<X> list = super.get(key);
    if (list == null) {
      list = newMap();
      put(key, list);
    }
    return list;
  }

  @SuppressWarnings("unchecked")
  protected StringTo<X> newMap() {
    return X_Collect.newStringMap(Class.class.cast(componentClass));
  }

}
