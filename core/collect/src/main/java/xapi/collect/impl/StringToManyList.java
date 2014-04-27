package xapi.collect.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;

public class StringToManyList <X> extends StringToAbstract<IntTo<X>> implements StringTo.Many<X>{

  private final Class<X> componentClass;

  public StringToManyList(Class<X> componentClass) {
    this.componentClass = componentClass;
  }

  @Override
  public final IntTo<X> get(String key) {
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
