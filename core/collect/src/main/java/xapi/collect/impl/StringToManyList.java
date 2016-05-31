package xapi.collect.impl;

import xapi.collect.X_Collect;
import xapi.collect.api.CollectionOptions;
import xapi.collect.api.IntTo;
import xapi.collect.api.StringTo;

import static xapi.collect.X_Collect.MUTABLE_LIST;

public class StringToManyList <X> extends StringToAbstract<IntTo<X>> implements StringTo.Many<X>{

  private static final long serialVersionUID = 681636065098625160L;
  private final Class<X> componentClass;
  private final CollectionOptions opts;

  public StringToManyList(final Class<X> componentClass) {
    this(componentClass, MUTABLE_LIST);
  }

  public StringToManyList(final Class<X> componentClass, CollectionOptions opts) {
    super(Class.class.cast(IntTo.class));
    this.componentClass = componentClass;
    this.opts = opts;
  }

  public StringToManyList(final Class<X> componentClass, final java.util.Map<String, IntTo<X>> map) {
    this(componentClass, map, MUTABLE_LIST);
  }

  public StringToManyList(final Class<X> componentClass, final java.util.Map<String, IntTo<X>> map, CollectionOptions opts) {
    super(Class.class.cast(IntTo.class), map);
    this.componentClass = componentClass;
    this.opts = opts;
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

  @Override
  public IntTo<X> newList() {
    return X_Collect.newList(componentClass, opts);
  }


}
