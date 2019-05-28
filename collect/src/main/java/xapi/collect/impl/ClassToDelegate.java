package xapi.collect.impl;

import java.util.Comparator;
import java.util.Map.Entry;

import javax.inject.Provider;

import xapi.collect.api.ClassTo;
import xapi.collect.api.IntTo;
import xapi.collect.proxy.CollectionProxy;

public abstract class ClassToDelegate <V>
extends ObjectToAbstract<Class<?>,V>
implements ClassTo<V>
{

  public static abstract class ManyAbstract <V> extends ClassToDelegate<IntTo<V>>
  implements ClassTo.Many<V>{

    @SuppressWarnings("unchecked")
    public ManyAbstract(
      Class<V> valueType,
      CollectionProxy<Class<?>,IntTo<V>> store,
      Provider<Iterable<Entry<Class<?>,IntTo<V>>>> iteratorProvider, Comparator<Class<?>> keyComparator,
      Comparator<IntTo<V>> valueComparator) {
      super(Class.class.cast(IntTo.class), store, iteratorProvider, keyComparator, valueComparator);
    }

  }

  @SuppressWarnings("unchecked")
  public ClassToDelegate(
    Class<V> valueType,
    CollectionProxy<Class<?>,V> store,
    Provider<Iterable<Entry<Class<?>,V>>> iteratorProvider, Comparator<Class<?>> keyComparator,
    Comparator<V> valueComparator) {
    super(Class.class.cast(Class.class), valueType, store, iteratorProvider, keyComparator, valueComparator);
  }

}
