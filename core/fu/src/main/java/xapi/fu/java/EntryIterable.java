package xapi.fu.java;

import xapi.fu.In1;
import xapi.fu.In1.In1Unsafe;
import xapi.fu.In2;
import xapi.fu.In2.In2Unsafe;
import xapi.fu.In2Out1;
import xapi.fu.MappedIterable;
import xapi.fu.Out2;
import xapi.fu.has.HasItems;

import java.util.Map.Entry;

public interface EntryIterable <K, V> extends HasItems<Out2<K, V>> {

  Iterable<Entry<K,V>> entries();

  @Override
  default MappedIterable<Out2<K, V>> forEachItem() {
    return MappedIterable.mapped(entries())
        .map(e->Out2.out2Immutable(e.getKey(), e.getValue()));
  }

  default MappedIterable<K> forEachKey() {
    return MappedIterable.mapped(entries())
        .map(Entry::getKey);
  }

  default MappedIterable<V> forEachValue() {
    return MappedIterable.mapped(entries())
        .map(Entry::getValue);
  }

  default EntryIterable <K, V> iterate(In2<K, V> callback) {
    entries().forEach(
        // Need to be explicit about the generics for some reason :-/
        callback.<Entry<K, V>>adapt(Entry::getKey, Entry::getValue)
            .toConsumer()
    );
    return this;

  }

  default void forBoth(In2<K, V> callback) {
    entries().forEach(callback.mapAdapter());
  }

  default <To> To findAndReduce(In2Out1<K, V, To> filter) {
    for (Entry<K, V> entry : entries()) {
      To result = filter.io(entry.getKey(), entry.getValue());
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  default void forValues(In1<V> callback) {
    entries().forEach(callback.<K>ignore1().mapAdapter());
  }
  default void forValuesUnsafe(In1Unsafe<V> callback) {
    entries().forEach(callback.<K>ignore1().mapAdapter());
  }
  default <I2> void forValuesUnsafe(In2Unsafe<V, I2> callback, I2 i2) {
    entries().forEach(callback
        .provide2(i2) // provide the supplied value
        .<K>ignore1() // ignore the key portion of entry iterator
        .mapAdapter() // translate to Consumer<Entry<K, V>>
    );
  }

}
