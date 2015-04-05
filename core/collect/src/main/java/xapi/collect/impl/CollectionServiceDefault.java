package xapi.collect.impl;

import static java.util.Collections.synchronizedMap;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import xapi.annotation.inject.SingletonDefault;
import xapi.collect.api.ClassTo;
import xapi.collect.api.CollectionOptions;
import xapi.collect.api.Fifo;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.collect.api.ObjectTo.Many;
import xapi.collect.api.StringDictionary;
import xapi.collect.api.StringTo;
import xapi.collect.proxy.CollectionProxy;
import xapi.collect.proxy.MapOf;
import xapi.collect.service.CollectionService;
import xapi.except.NotYetImplemented;
import xapi.platform.AndroidPlatform;
import xapi.platform.GwtDevPlatform;
import xapi.platform.JrePlatform;
import xapi.util.X_Runtime;

@GwtDevPlatform
@JrePlatform
@AndroidPlatform
@SingletonDefault(implFor=CollectionService.class)
public class CollectionServiceDefault implements CollectionService{

  static final Comparator<String> STRING_CMP = new Comparator<String>() {
    @Override
    public int compare(final String o1, final String o2) {
      if (o1 == null) {
        return o2 == null ? 0 : "".compareTo(o2);
      }
      return o1.compareTo(o2 == null ? "" : o2);
    }
  };
  static final Comparator<Enum<?>> ENUM_CMP = new Comparator<Enum<?>>() {
    @Override
    public int compare(final Enum<?> o1, final Enum<?> o2) {
      if (o1 == null) {
        return o2 == null ? 0 : -o2.ordinal();
      }
      return o1.ordinal() - (o2 == null ? 0 : o2.ordinal());
    }
  };
  public static final Comparator<Class<?>> CLASS_CMP = new Comparator<Class<?>>() {
    @Override
    public int compare(final Class<?> o1, final Class<?> o2) {
      if (o1 == null) {
        return o2 == null ? 0 : -o2.hashCode();
      }
      return o1.hashCode() - (o2 == null ? 0 : o2.hashCode());
    }
  };
  static final Comparator<Number> NUMBER_CMP = new Comparator<Number>() {
    @Override
    public int compare(Number o1, Number o2) {
      if (o1==null) {
        o1=0;
      }
      if (o2==null) {
        o2=0;
      }
      final double delta = o1.doubleValue() - o2.doubleValue();
      if (Math.abs(delta)<0.0000000001) {
        return 0;
      }
      return delta < 0 ? -1 : 1;
    }
  };
  static final Comparator<Object> OBJECT_CMP = new Comparator<Object>() {
    @Override
    @SuppressWarnings({"unchecked","rawtypes"})
    public int compare(final Object o1, final Object o2) {
      if (o1 instanceof Comparable) {
        return ((Comparable)o1).compareTo(o2);
      }
      return System.identityHashCode(o1) - System.identityHashCode(o2);
    }
  };

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private final CollectionProxy<Class<?>,Comparator<?>> comparators =
    newProxy((Class)Class.class, (Class)Comparator.class, CollectionOptions.asConcurrent(true).build());


  public CollectionServiceDefault() {
    comparators.entryFor(Object.class).setValue(OBJECT_CMP);
    comparators.entryFor(String.class).setValue(STRING_CMP);
    comparators.entryFor(Enum.class).setValue(ENUM_CMP);
    comparators.entryFor(Class.class).setValue(CLASS_CMP);
    comparators.entryFor(Number.class).setValue(NUMBER_CMP);
    comparators.entryFor(Byte.class).setValue(NUMBER_CMP);
    comparators.entryFor(Short.class).setValue(NUMBER_CMP);
    comparators.entryFor(Integer.class).setValue(NUMBER_CMP);
    comparators.entryFor(Long.class).setValue(NUMBER_CMP);
    comparators.entryFor(Float.class).setValue(NUMBER_CMP);
    comparators.entryFor(Double.class).setValue(NUMBER_CMP);
  }

  protected <K, V> Map<K,V> newMap(final CollectionOptions opts) {
    if (opts.insertionOrdered()) {
      if (opts.concurrent()) {
        // TODO: something with better performance...
        // Perhaps supersource ConcurrentSkipListMap,
        // and remove all the contention-worries that plague its implementation
        return synchronizedMap(new LinkedHashMap<K, V>());
      } else {
        return new LinkedHashMap<K, V>();
      }
    }
    if (X_Runtime.isMultithreaded()) {
      return new ConcurrentHashMap<K,V>();
    } else {
      return new HashMap<K,V>();
    }
  }

  @Override
  public <V> IntTo<V> newList(final Class<? extends V> cls, final CollectionOptions opts) {
    return new IntToList<V>(cls);
  }

  @Override
  public <V> IntTo<V> newSet(final Class<V> cls, final CollectionOptions opts) {
    throw new NotYetImplemented("IntToSet not yet implemented");
  }

  @Override
  public <K,V> ObjectTo<K,V> newMap(final Class<K> key, final Class<V> cls, final CollectionOptions opts) {
    return new MapOf<K,V>(this.<K, V>newMap(opts), key, cls);
  }


  @Override
  public <V> ClassTo<V> newClassMap(final Class<V> cls, final CollectionOptions opts) {
    return new ClassToDefault<V>(this.<Class<?>, V>newMap(opts), cls);
  }

  @Override
  public <V> StringTo<V> newStringMap(final Class<? extends V> cls, final CollectionOptions opts) {
    return new StringToAbstract<V>(this.<String, V>newMap(opts));
  }

  protected <K, V> CollectionProxy<K,V> newProxy(final Class<K> keyType, final Class<V> valueType, final CollectionOptions opts) {
    if (opts.insertionOrdered()) {
      if (opts.concurrent()) {
        return new MapOf<K,V>(new ConcurrentSkipListMap<K,V>(), keyType, valueType);
      } else {
        return new MapOf<K,V>(new LinkedHashMap<K,V>(), keyType, valueType);
      }
    }
    if (opts.concurrent()) {
      return new MapOf<K,V>(new ConcurrentHashMap<K,V>(), keyType, valueType);
    } else {
      return new MapOf<K,V>(new HashMap<K,V>(), keyType, valueType);
    }
  }

  @SuppressWarnings({
      "unchecked", "unused"
  })
  private <V> Comparator<V> getComparator(Class<?> cls) {
    Comparator<?> cmp = null;
    while ((cmp = comparators.get(cls))==null) {
      cls = cls.getSuperclass();
    }
    return (Comparator<V>)cmp;
  }

  @Override
  public <K,V> Many<K,V> newMultiMap(final Class<K> key, final Class<V> cls, final CollectionOptions opts) {
    throw new NotYetImplemented("Multi-map not yet implemented");
  }

  @Override
  public <V> xapi.collect.api.ClassTo.Many<V> newClassMultiMap(final Class<V> cls, final CollectionOptions opts) {
    throw new NotYetImplemented("Multi-map not yet implemented");
  }

  @Override
  public <V> xapi.collect.api.StringTo.Many<V> newStringMultiMap(final Class<V> cls,
    final CollectionOptions opts) {
    throw new NotYetImplemented("Multi-map not yet implemented");
  }

  @Override
  public <V> StringDictionary<V> newDictionary() {
    return new StringDictionaryDefault<V>();
  }

  @Override
  public <V> Fifo<V> newFifo() {
    return new SimpleFifo<V>();
  }
}
