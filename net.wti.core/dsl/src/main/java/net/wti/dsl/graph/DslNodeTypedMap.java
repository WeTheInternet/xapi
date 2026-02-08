package net.wti.dsl.graph;

import net.wti.dsl.api.DslObject;
import net.wti.dsl.type.DslType;
import net.wti.dsl.type.DslTypeTypedMap;

import java.util.List;

///
/// DslNodeTypedMap:
///
/// Runtime node produced when analyzing a value under a {@link DslTypeTypedMap} schema.
///
/// A typedMap node models a schema-controlled *instruction-list* of entries:
///  - entries are ordered,
///  - repeated keys are allowed,
///  - keys are restricted to those declared by the schema type.
///
/// Values are represented as {@link DslObject} so they may be either leaf values
/// (DslValue implementations) or nested nodes (DslNode implementations).
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 02:09
public interface DslNodeTypedMap extends DslNode {

    /**
     * A single typedMap instruction entry: key + normalized value.
     */
    final class Entry {
        private final String key;
        private final DslObject value;

        public Entry(final String key, final DslObject value) {
            if (key == null || key.isEmpty()) {
                throw new IllegalArgumentException("key must not be null/empty");
            }
            if (value == null) {
                throw new IllegalArgumentException("value must not be null");
            }
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public DslObject getValue() {
            return value;
        }
    }

    /**
     * @return the schema type that declares the allowed key set and per-key value types.
     */
    DslTypeTypedMap getType();

    /**
     * @return ordered instruction entries (repeated keys allowed).
     */
    List<Entry> getEntries();

    /**
     * Convenience: returns the declared per-key type from {@link #getType()}.
     */
    DslType getDeclaredType(String key);

    /**
     * @return ordered values for a given key (repeated keys allowed). Unknown keys return an empty list.
     */
    List<DslObject> getValues(String key);
}
