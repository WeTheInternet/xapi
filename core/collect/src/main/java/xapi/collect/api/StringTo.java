package xapi.collect.api;

import xapi.fu.In1;
import xapi.fu.In1Out1;
import xapi.fu.In1Out1.In1Out1Unsafe;
import xapi.fu.MapLike;
import xapi.fu.MappedIterable;
import xapi.fu.Out2;
import xapi.fu.iterate.SizedIterable;

import java.io.Serializable;

import static xapi.fu.In2.in2;

/**
 * StringTo is a special mapping interface,
 * since it has the best possible native support in dictionary-oriented
 * languages, like javascript, we do not extend ObjectTo, which
 * forces generic override problems in the GWT compiler,
 * rather, we tie in to the HasValues interface instead.
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 * @param <V>
 */
public interface StringTo <V>
extends HasValues<String,V>, Serializable, MapLike<String, V>
{

  String[] keyArray();

  default int size() {
    return keyArray().length;
  }

  default V getOrCreateUnsafe(String key, In1Out1Unsafe<String, V> factory) {
    return getOrCreate(key, factory);
  }

  default V getOrCreate(String key, In1Out1<String, V> factory) {
    V value = get(key);
    if (value == null) {
      value = factory.io(key);
      put(key, value);
    }
    return value;
  }

  @Override
  default SizedIterable<Out2<String, V>> forEachItem() {
    return MapLike.super.forEachItem();
  }

  interface Many <V> extends StringTo<IntTo<V>>, HasMany<String, V> {
    @Override
    default SizedIterable<Out2<String, IntTo<V>>> forEachItem() {
      return HasMany.super.forEachItem();
    }

    default MappedIterable<V> flattenedValues() {
      return mappedValues()
          .flatten(IntTo::forEach);
    }

  }

  default In1<V> adapter(In1Out1<V, String> adapter) {
    return in2(this::put).adapt1(adapter);
  }

}
