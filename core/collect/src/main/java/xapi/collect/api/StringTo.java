package xapi.collect.api;

import xapi.fu.In1Out1;

import java.io.Serializable;

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
extends HasValues<String,V>, Serializable
{

  V get(String key);
  V put(String key, V value);
  V remove(String key);

  String[] keyArray();

  int size();

  default V getOrCreate(String key, In1Out1<String, V> factory) {
    V value = get(key);
    if (value == null) {
      value = factory.io(key);
      put(key, value);
    }
    return value;
  }

  interface Many <V>
  extends StringTo<IntTo<V>>
  {
    Many <V> add(String key, V value);
  }

}
