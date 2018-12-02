package xapi.fu.java;

import xapi.fu.In1Out1;
import xapi.fu.data.*;
import xapi.fu.api.GwtIncompatible;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.Collections.synchronizedMap;

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

    public static <K, V> JdkMultiSet<K, V> toMultiSet(Map<K, SetLike<V>> map, In1Out1<K, SetLike<V>> setFactory) {
        return new JdkMultiSet<>(map, setFactory);
    }

    public static <K, V> JdkMultiList<K, V> toMultiList(Map<K, ListLike<V>> map, In1Out1<K, ListLike<V>> setFactory) {
        return new JdkMultiList<>(map, setFactory);
    }

    public static <V> List<V> itrToList(Iterable<? extends V> items) {
        if (items instanceof List) {
            return (List<V>) items;
        }
        final ArrayList<V> list = new ArrayList<>();
        items.forEach(list::add);
        return list;
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

    public static <K, V> JdkMultiList<K, V> multiList() {
        return toMultiList(defaultMap(), defaultListFactory());
    }

    public static <K, V> JdkMultiList<K, V> multiListOrderedInsertion() {
        return toMultiList(defaultMapOrderedInsertion(), defaultListFactory());
    }

    public static <K, V> JdkMultiSet<K, V> multiSet() {
        return toMultiSet(defaultMap(), defaultSetFactory());
    }

    @GwtIncompatible
    public static <V> ListLike<V> listArrayConcurrent() {
        return toList(new CopyOnWriteArrayList<>());
    }

    public static <V> ListLike<V> list() {
        return toList(defaultList());
    }

    public static <V> ListLike<V> listArray() {
        return toList(new ArrayList<>());
    }

    public static <V> ListLike<V> listLinked() {
        return toList(new LinkedList<>());
    }

    public static <V> SetLike<V> set() {
        return toSet(defaultSet());
    }

    public static <V> SetLike<V> setHash() {
        return toSet(new HashSet<>());
    }

    public static <V> SetLike<V> setHashConcurrent() {
        return toSet(new ConcurrentHashMap<>());
    }

    public static <V> SetLike<V> setHashIdentity() {
        return toSet(new IdentityHashMap<>());
    }

    public static <V> SetLike<V> setHashIdentitySynchronized() {
        return toSet(synchronizedMap(new IdentityHashMap<>()));
    }

    public static <V> SetLike<V> setLinked() {
        return toSet(new LinkedHashSet<V>());
    }

    public static <V> SetLike<V> setLinkedSynchronized() {
        return toSet(Collections.synchronizedSet(new LinkedHashSet<V>()));
    }

    public static boolean isEmpty(Collection<?> resources) {
        return resources == null || resources.isEmpty();
    }

    public static <K, V> Map<K, V> defaultMap() {
        return new HashMap<>();
    }

    public static <K, V> Map<K, V> defaultMapOrderedInsertion() {
        return new LinkedHashMap<>();
    }

    public static <V> Set<V> defaultSet() {
        return new HashSet<>();
    }

    public static <V> List<V> defaultList() {
        return new ArrayList<>();
    }

    public static <K, V> In1Out1<K, ListLike<V>> defaultListFactory() {
        In1Out1 cheat = DEFAULT_LIST;
        return cheat;
    }

    public static <K, V> In1Out1<K, SetLike<V>> defaultSetFactory() {
        In1Out1 cheat = DEFAULT_SET;
        return cheat;
    }

    private static final In1Out1<Object, ListLike> DEFAULT_LIST = In1Out1.ofDeferred(X_Jdk::list);
    private static final In1Out1<Object, SetLike> DEFAULT_SET = In1Out1.ofDeferred(X_Jdk::set);

    public static <V> List<V> asList(ListLike<V> flatten) {
        if (flatten instanceof ListAdapter) {
            return ((ListAdapter<V>) flatten).getList();
        }
        final List<V> list = defaultList();
        flatten.forAll(list::add);
        return list;
    }
}
