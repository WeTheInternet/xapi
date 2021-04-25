package xapi.collect.impl;

import xapi.annotation.inject.SingletonDefault;
import xapi.collect.api.ClassTo;
import xapi.collect.api.CollectionOptions;
import xapi.collect.api.Fifo;
import xapi.collect.api.IntTo;
import xapi.collect.api.ObjectTo;
import xapi.collect.api.ObjectTo.Many;
import xapi.collect.api.StringDictionary;
import xapi.collect.api.StringTo;
import xapi.collect.proxy.api.CollectionProxy;
import xapi.collect.proxy.impl.MapOf;
import xapi.collect.service.CollectionService;
import xapi.except.NotYetImplemented;
import xapi.platform.AndroidPlatform;
import xapi.platform.GwtDevPlatform;
import xapi.platform.JrePlatform;
import xapi.util.X_Runtime;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static java.util.Collections.synchronizedMap;

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
    newProxy(Class.class, Comparator.class, CollectionOptions.asConcurrent(true).build());


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
    if (opts.multiplex() != null) {
      throw new NotYetImplemented("Multiplexing");
    }
    if (opts.identityKeys()) {
      throw new NotYetImplemented("Identity Keys");
    }
    if (!opts.mutable()) {
      return Collections.unmodifiableMap(newMap(CollectionOptions.from(opts)
        .mutable(true).build()));
    }
    if (opts.insertionOrdered()) {
      if (opts.concurrent()) {
        // TODO: something with better performance...
        return synchronizedMap(new LinkedHashMap<>());
      } else {
        return new LinkedHashMap<>();
      }
    }
    if (opts.keyOrdered()) {
      if (opts.concurrent()) {
        return new ConcurrentSkipListMap<>();
      } else {
        return new TreeMap<>();
      }
    }
    if (X_Runtime.isMultithreaded()) {
      return new ConcurrentHashMap<>();
    } else {
      return new HashMap<>();
    }
  }

  @Override
  public <Type, Generic extends Type>  IntTo<Type> newList(final Class<Generic> cls, final CollectionOptions opts) {
    return opts.concurrent() ? new IntToListConcurrent<>(cls) : new IntToList<>(cls);
  }

  @Override
  public <E, Generic extends E> IntTo<E> newSet(
      Class<Generic> cls, CollectionOptions opts
  ) {
    return new IntToSet<>(cls, opts);
  }

  @Override
  public <K,V, Key extends K, Value extends V> ObjectTo<K,V> newMap(final Class<Key> key, final Class<Value> cls, final CollectionOptions opts) {
    return new MapOf<>(this.<K, V>newMap(opts), key, cls);
  }


  @Override
  public <V, Generic extends V> ClassTo<V> newClassMap(final Class<Generic> cls, final CollectionOptions opts) {
    return new ClassToDefault<V>(this.<Class<?>, V>newMap(opts), cls);
  }

  @Override
  public <V, Generic extends V> StringTo<V> newStringMap(final Class<Generic> cls, final CollectionOptions opts) {
    return new StringToAbstract<>(cls, this.<String, V>newMap(opts));
  }

  public <K, V, Key extends K, Value extends V> CollectionProxy<K, V> newProxy(
      Class<Key> keyType, Class<Value> valueType, CollectionOptions opts) {
    if (opts.insertionOrdered()) {
      if (opts.concurrent()) {
        return new MapOf<>(synchronizedMap(new LinkedHashMap<>()), keyType, valueType);
      } else {
        return new MapOf<>(new LinkedHashMap<>(), keyType, valueType);
      }
    }
    if (opts.concurrent()) {
      return new MapOf<>(new ConcurrentHashMap<>(), keyType, valueType);
    } else {
      return new MapOf<>(new HashMap<>(), keyType, valueType);
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
    return new ObjectToManyList<>(key, cls, this.<K, IntTo<V>>newMap(opts));
  }

  @Override
  public <V, Generic extends V> ClassTo.Many<V> newClassMultiMap(final Class<Generic> cls, final CollectionOptions opts) {
    return new ClassToManyList<>(cls, this.<Class<?>, IntTo<V>>newMap(opts));
  }

  @Override
  public <V, Generic extends V> StringTo.Many<V> newStringMultiMap(final Class<Generic> cls,
    final CollectionOptions opts) {
    // TODO honor opts
    if (opts.insertionOrdered()) {
      return new StringToManyList<>(cls,
          // ick... we'll get a fast, insertion ordered concurrent map.  For now, just make it synchronized :-(
          opts.concurrent() ? synchronizedMap(new LinkedHashMap<>()) : new LinkedHashMap<>()
      , opts);
    }
    if (opts.keyOrdered()) {
      return new StringToManyList<>(cls,
          opts.concurrent() ? new ConcurrentSkipListMap<>() : new TreeMap<>()
      , opts);

    }
    return new StringToManyList<>(cls, opts);
  }

  @Override
  public <V> StringDictionary<V> newDictionary(Class<V> cls) {
    return new StringDictionaryDefault<V>(cls);
  }

  @Override
  public <V> Fifo<V> newFifo() {
    return new SimpleFifo<V>();
  }

}
