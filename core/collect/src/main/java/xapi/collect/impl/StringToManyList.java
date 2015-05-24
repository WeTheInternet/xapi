package xapi.collect.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;

public class StringToManyList <X> extends StringToAbstract<IntTo<X>> implements StringTo.Many<X>{

  private static final long serialVersionUID = 681636065098625160L;
  private final Class<X> componentClass;

  public StringToManyList(final Class<X> componentClass) {
    this.componentClass = componentClass;
  }

  public StringToManyList(final Class<X> componentClass, final java.util.Map<String, IntTo<X>> map) {
    super(map);
    this.componentClass = componentClass;
  }

  @Override
  public StringToManyList<X> add(final String key, final X value) {
    get(key).add(value);
    return this;
  }

  @Override
  public final IntTo<X> get(final String key) {
    IntTo<X> list = super.get(key);
    if (list == null) {
      list = newList();
      put(key, list);
    }
    return list;
  }

  protected IntTo<X> newList() {
    return X_Collect.newList(componentClass);
  }


}
