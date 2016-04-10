package xapi.collect.api;

import xapi.fu.In1;
import xapi.fu.In1Out1;

import static xapi.fu.In2.in2;

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
extends HasValues<String,V>, Serializable, TypedMap<String, V>
{

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

  default In1<V> adapter(In1Out1<V, String> adapter) {
    return in2(this::put).adapt1(adapter);
  }

}
