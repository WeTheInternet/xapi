package xapi.collect.api;

import xapi.fu.In1;
import xapi.fu.In1Out1;
import xapi.fu.In2;
import xapi.fu.In3;
import xapi.fu.itr.CachingIterator;
import xapi.fu.itr.MappedIterable;
import xapi.fu.itr.SizedIterable;
import xapi.util.api.ReceivesValue;

/**
 * A simple dictionary interface, that avoids overwriting java.util.Map
 * interface methods, and can bind to whatever native construct
 * best supports an Object-to-Object mapping. JSOs in javascript, HashMap in jre
 *
 * Note that JSO support requires a key-transformer for everything but String;
 * if you want maximum runtime performance, use {@link xapi.gwt.collect.StringToGwt},
 * or {@link xapi.collect.X_Collect::newStringDictionary()}
 *
 * @author "James X. Nelson (james@wetheinter.net)"
 *
 * @param <K> -
 * @param <V>
 */
public interface Dictionary <K, V> {

  boolean hasKey(K key);

  V getValue(K key);

  V setValue(K key, V value);

  V removeValue(K key);

  void clearValues();

  void forKeys(ReceivesValue<K> receiver);
  default void keys(In1<K> in) {
    forKeys(in::in);
  }
  default MappedIterable<K> getKeys() {
    return MappedIterable.fromAsync(In1.in1(this::keys));
  }

  default void forEach(In2<K, V> in) {
    forKeys(in.adapt2(this::getValue)::in);
  }

  default <E1> void forEach(In3<K, V, E1> in, E1 extra) {
    forEach(in.provide3(extra));
  }

}
