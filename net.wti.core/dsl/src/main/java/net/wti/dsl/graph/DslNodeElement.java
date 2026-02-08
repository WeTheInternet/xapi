package net.wti.dsl.graph;

import net.wti.dsl.api.DslObject;

import java.util.List;

///
/// DslNodeElement:
///
/// A normalized element node in the runtime DSL graph.
///
/// An element node models:
///  - a tag name (the element name),
///  - an ordered list of attribute entries,
///  - an ordered list of child elements.
///
/// ## Ordering and duplicates
/// Attribute entries are modeled as an *ordered instruction-list* rather than a Map:
///  - ordering is preserved exactly as encountered in source,
///  - repeated attribute names are allowed (some DSLs use repeated keys as additive semantics),
///  - each entry retains its own source provenance via the contained DslObject.
///
/// Children are also an ordered list for the same ordering/provenance reasons.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 00:42
public interface DslNodeElement extends DslNode {

    /**
     * A single attribute entry: name + normalized value.
     *
     * Values are modeled as {@link DslObject} so they may be either:
     *  - a {@code DslValue} (leaf value), or
     *  - another {@link DslNode} (nested node).
     */
    final class AttrEntry {
        private final String name;
        private final DslObject value;

        public AttrEntry(final String name, final DslObject value) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("name must not be null/empty");
            }
            if (value == null) {
                throw new IllegalArgumentException("value must not be null");
            }
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public DslObject getValue() {
            return value;
        }
    }

    /**
     * @return the element tag name (never null/empty).
     */
    String getTagName();

    /**
     * @return ordered attribute entries (repeated names allowed).
     */
    List<AttrEntry> getAttributes();

    /**
     * @return ordered child elements.
     */
    List<DslNodeElement> getChildren();
}
