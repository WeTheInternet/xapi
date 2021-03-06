package xapi.collect.proxy.api;

import xapi.collect.api.CollectionOptions;
import xapi.fu.*;
import xapi.fu.data.MapLike;
import xapi.fu.has.HasEmptiness;
import xapi.collect.api.ObjectTo;
import xapi.collect.simple.SimpleStack;
import xapi.fu.In2.In2Unsafe;
import xapi.fu.itr.ArrayIterable;
import xapi.fu.itr.SizedIterable;
import xapi.fu.itr.MappedIterable;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An interface that can be compatible with jdk collection types.
 *
 * TODO: remove this type so we don't have terrible, ambiguous method calls
 * which take Object instead of a typed parameter.
 *
 * All types which currently extend this type should instead extend something more universal,
 * like {@link MapLike}
 *
 * @param <K> - The key type
 * @param <V> - The value type
 */
public interface CollectionProxy <K, V> extends HasEmptiness
{

  ObjectTo<K, V> clone(CollectionOptions options);

  V put(Entry<K,V> item);

  Entry<K,V> entryFor(Object key);

  default Entry<K,V> entryFor(Object key, V value) {
      final Entry<K, V> entry = entryFor(key);
      entry.setValue(value);
      return entry;
  }

  V get(Object key);

  default Maybe<V> getMaybe(K key) {
      V value = get(key);
      return Maybe.nullable(value);
  }

  void setValue(Object key, Object value);

  default void copyFrom(CollectionProxy<K, V> other) {
      other.readWhileTrue((k, v)->{
          entryFor(other)
              .setValue(v);
          return true;
      });
  }

  V remove(Object key);

  int size();

  V[] toArray();

  Collection<V> toCollection(Collection<V> into);

  Map<K, V> toMap(Map<K, V> into);

  void clear();

  Class<K> keyType();

  Class<V> valueType();

   boolean readWhileTrue(In2Out1<K, V, Boolean> callback);

   default boolean readWhileTrue(In1<Out2<K, V>> callback, In2Out1<K, V, Boolean> filter) {
       @SuppressWarnings("Convert2Lambda")
       // for some terrible reason, gwt compiler gets an npe trying to compile this
       // lambda inside a default method from a JSO type (IntToGwt?)
       // while it would be nice to fix this compiler error, we are glossing over
       // it for the time being by using an explicit anonymous class.
       final In2Out1<K, V, Boolean> test = new In2Out1<K, V, Boolean>() {
           @Override
           public Boolean io(K k, V v) {
               if (filter.io(k, v)) {
                   Out2<K, V> o = Out2.out2Immutable(k, v);
                   callback.in(o);
                   return true;
               }
               return false;
           }
       };
       return readWhileTrue(
         test
     );
   }

   default void forEachValue(In1<V> callback) {
     // purposely create a copy. This will avoid comodification exception
     for (V v : toArray()) {
       callback.in(v);
     }
   }

   default void forEachPairUnsafe(In2Unsafe<K, V> callback) {
       forEachPair(callback);
   }
   default void forEachPair(In2<K, V> callback) {
     // purposely create a copy. This will avoid comodification exception
     readWhileTrue(callback.supply1(true));
   }

   default SizedIterable<Out2<K, V>> forEachEntry() {
     SimpleStack<Out2<K, V>> stack = new SimpleStack<>();
     int[] cnt = {0};
     readWhileTrue(item->{
         stack.add(item);
         cnt[0]++;
     }, In2Out1.RETURN_TRUE);
     return SizedIterable.of(cnt[0], stack);
   }

  default SizedIterable<V> iterateValues() {
    final V[] arr = toArray();
    return new ArrayIterable<>(arr);
  }

  default String toSource() {
      StringBuilder b = new StringBuilder("{");
      String sep = "";
      for (Out2<K, V> entry : forEachEntry()) {
          b.append(sep);
          sep = ", ";
          b.append(toString(entry.out1(), entry.out2()));
      }
      b.append("}");
      return b.toString();
  }

  default String toString(K key, V value) {
      return key + "=" + value ;
  }

   default MappedIterable<V> filterKeysReturnValue(In1Out1<K, Boolean> filter) {
       return filterKeys(filter)
           .map(Out2::out2);
   }
   default MappedIterable<Out2<K, V>> filterKeys(In1Out1<K, Boolean> filter) {
       return forEachEntry()
                .filter(o->filter.io(o.out1()));

   }

    default Do removeKeyLater(K value) {
       return ()->remove(value);
    }
}
