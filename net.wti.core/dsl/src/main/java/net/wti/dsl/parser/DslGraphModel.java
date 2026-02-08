package net.wti.dsl.parser;

import net.wti.dsl.type.DslType;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

///
/// DslGraphModel:
///
/// Immutable, high-level description of the object graph we will generate for a DSL.
///
///  - dslName:     logical DSL name (from DslModel.getName()).
///  - packageName: Java package to use for generated graph types.
///  - rootTag:     the tag name that represents the root DSL element.
///  - elements:    ordered map of element tag -> ElementType, preserving DSL declaration order.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 16/12/2025 @ 01:44
public final class DslGraphModel {

    private final String dslName;
    private final String packageName;
    private final String rootTag;
    private final Map<String, ElementType> elementsByTag;

    public DslGraphModel(String dslName,
                         String packageName,
                         String rootTag,
                         Map<String, ElementType> elementsByTag) {
        this.dslName = Objects.requireNonNull(dslName, "dslName must not be null");
        this.packageName = packageName == null ? "" : packageName;
        this.rootTag = Objects.requireNonNull(rootTag, "rootTag must not be null");
        Objects.requireNonNull(elementsByTag, "elementsByTag must not be null");
        this.elementsByTag = Collections.unmodifiableMap(new LinkedHashMap<>(elementsByTag));
    }

    public String getDslName() {
        return dslName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getRootTag() {
        return rootTag;
    }

    ///
    /// @return Ordered, unmodifiable map of element tag name -> ElementType.
    ///
    public Map<String, ElementType> getElementsByTag() {
        return elementsByTag;
    }

    ///
    /// Convenience accessor to get a single element definition by tag name.
    ///
    public ElementType getElement(String tagName) {
        return elementsByTag.get(tagName);
    }

    ///
    /// ElementType:
    ///
    /// Describes a single tag in the DSL, and how it should map to a generated *El type.
    ///
    ///  - tagName:         the raw DSL tag name (e.g. "root", "entity-field").
    ///  - elTypeName:      simple Java class name for the generated immutable element type.
    ///  - attributes:      set of attribute names this element may legally have.
    ///  - attributeTypes:  ordered map of attribute name -> compiled DslType schema for that attribute.
    ///
    public static final class ElementType {

        private final String tagName;
        private final String elTypeName;
        private final Set<String> attributes;
        private final Map<String, DslType> attributeTypes;

        public ElementType(
                final String tagName,
                final String elTypeName,
                final Set<String> attributes
        ) {
            this(tagName, elTypeName, attributes, Collections.<String, DslType>emptyMap());
        }

        public ElementType(
                final String tagName,
                final String elTypeName,
                final Set<String> attributes,
                final Map<String, ? extends DslType> attributeTypes
        ) {
            this.tagName = Objects.requireNonNull(tagName, "tagName must not be null");
            this.elTypeName = Objects.requireNonNull(elTypeName, "elTypeName must not be null");

            Objects.requireNonNull(attributes, "attributes must not be null");
            this.attributes = Collections.unmodifiableSet(new LinkedHashSet<>(attributes));

            Objects.requireNonNull(attributeTypes, "attributeTypes must not be null");
            this.attributeTypes = Collections.unmodifiableMap(new LinkedHashMap<>(attributeTypes));
        }

        public String getTagName() {
            return tagName;
        }

        public String getElTypeName() {
            return elTypeName;
        }

        public Set<String> getAttributes() {
            return attributes;
        }

        ///
        /// @return ordered attribute schema: attribute name -> DslType.
        ///
        public Map<String, DslType> getAttributeTypes() {
            return attributeTypes;
        }

        public DslType getAttributeType(final String attributeName) {
            return attributeTypes.get(attributeName);
        }
    }
}