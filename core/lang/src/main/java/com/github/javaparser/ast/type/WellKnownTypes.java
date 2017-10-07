package com.github.javaparser.ast.type;

/**
 * A utility for types that we will automatically discover packagenames for.
 *
 * While a little bit dirty, the extra convenience and cleaner code is worth it, imho.
 *
 * Created by James X. Nelson (james @wetheinter.net) on 9/24/17.
 */
public class WellKnownTypes {
    public static String qualifyType(String s) {
        String raw = s.split("<")[0];
        if (s.contains(".")) {
            return s;
        }
        switch (raw) {
            case "Iterator":
            case "List":
            case "Set":
            case "ArrayList":
            case "LinkedList":
            case "Queue":
            case "Dequeue":
            case "HashSet":
            case "TreeSet":
            case "Map":
            case "HashMap":
            case "TreeMap":
                return "java.util." + s;
            case "ConcurrentMap":
            case "ConcurrentHashMap":
            case "ConcurrentSkipListMap":
            case "ConcurrentLinkedDeque":
                return "java.util.concurrent." + s;
            case "MapLike":
            case "ListLike":
            case "SetLike":
                return "xapi.fu." + s;
            case "MappedIterable":
            case "Chain":
            case "ChainBuilder":
            case "ArrayIterable":
            case "CachingIterator":
                return "xapi.fu.iterate." + s;
            case "IntTo":
            case "StringTo":
            case "StringTo.Many":
            case "ClassTo":
            case "ClassTo.Many":
            case "ObjectTo":
            case "ObjectTo.Many":
            case "Fifo":
                return "xapi.collect.api." + s;
            case "ModelKey":
            case "Model":
            case "ModelBuilder":
                return "xapi.model.api." + s;
        }
        return s;
    }
}
