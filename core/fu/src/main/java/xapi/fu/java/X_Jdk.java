package xapi.fu.java;

import xapi.fu.ListLike;
import xapi.fu.MapLike;
import xapi.fu.SetLike;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A helper class which uses jdk-standard objects, like HashMap, ArrayList, etc.
 *
 * We try to keep these separate from core code,
 * since some platforms may wish to use their own storage APIs.
 *
 * Code in core packages should avoid using this,
 * but, once you are in, say, a known java jvm environment,
 * you can use these helpers more liberally.
 *
 * Gwt already contains super-source of this, erasing the concurrent type references.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 7/21/17.
 */
public class X_Jdk {

    public static <K, V> MapLike<K, V> toMap(Map<K, V> map) {
        return new MapAdapter<>(map);
    }

    public static <V> ListLike<V> toList(List<V> list) {
        return new ListAdapter<>(list);
    }

    public static <V> SetLike<V> toSet(Set<V> list) {
        return new SetAdapter<>(list);
    }

    public static <V> SetLike<V> toSet(Map<V, ?> map) {
        return new SetAdapter<>(map, null);
    }

    public static <K, V> MapLike<K, V> mapHash() {
        return toMap(new HashMap<>());
    }

    public static <K, V> MapLike<K, V> mapHashConcurrent() {
        return toMap(new ConcurrentHashMap<>());
    }

    public static <K, V> MapLike<K, V> mapIdentity() {
        return toMap(new IdentityHashMap<>());
    }

    public static <K, V> MapLike<K, V> mapWeak() {
        return toMap(new WeakHashMap<K, V>());
    }

    public static <K, V> MapLike<K, V> mapOrderedInsertion() {
        return toMap(new LinkedHashMap<>());
    }

    public static <K, V> MapLike<K, V> mapOrderedKey() {
        return toMap(new TreeMap<>());
    }

    public static <K, V> MapLike<K, V> mapOrderedKeyConcurrent() {
        return toMap(new ConcurrentSkipListMap<>());
    }

    public static <V> ListLike<V> listArrayConcurrent() {
        return toList(new CopyOnWriteArrayList<>());
    }

    public static <V> ListLike<V> listArray() {
        return toList(new ArrayList<>());
    }

    public static <V> ListLike<V> listLinked() {
        return toList(new LinkedList<>());
    }

    public static <V> SetLike<V> setHash() {
        return toSet(new HashSet<>());
    }

    public static <V> SetLike<V> setHashConcurrent() {
        return toSet(new ConcurrentHashMap<>());
    }

    public static <V> SetLike<V> setLinked() {
        return toSet(new LinkedHashSet<V>());
    }

}
