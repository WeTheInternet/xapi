package net.wti.dsl.schema;

import net.wti.dsl.type.DslType;
import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

///
/// DslSchemaElement:
///
/// A compiled schema declaration for a single DSL element/tag (an <element-def .../> entry).
///
/// Elements are the core units of DSL instance documents.
/// Each element declares:
///  - its tag name,
///  - its allowed attributes (with types + constraints),
///  - its allowed child elements (first-pass: tag -> element-ref type).
///
/// ## Attribute storage
/// Elements keep their own attribute map for fast lookups during generation/validation.
/// These entries typically point at shared {@link DslSchemaAttribute} instances held in
/// {@link DslSchema#getAttributes()}, enabling reuse-by-name.
///
/// ## Child rules
/// For now, children are represented as a map from child tag name -> {@link DslType},
/// because existing DSL definitions commonly use `<element-ref name="childType" />`
/// which compiles naturally into {@link net.wti.dsl.type.DslTypeElement}.
///
/// As the DSL-definition language grows, this can evolve into richer child-rule objects
/// without changing the schema’s overall layering.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 03:12
public final class DslSchemaElement extends ImmutableDslSchemaObject {

    private final String tagName;
    private final Map<String, DslSchemaAttribute> attributesByName;
    private final Map<String, DslType> childrenByTag;

    public DslSchemaElement(
            final MappedIterable<Expression> sourceAst,
            final String tagName,
            final Map<String, ? extends DslSchemaAttribute> attributesByName,
            final Map<String, ? extends DslType> childrenByTag
    ) {
        super(sourceAst);
        if (tagName == null || tagName.isEmpty()) {
            throw new IllegalArgumentException("tagName must not be null/empty");
        }
        this.tagName = tagName;

        Objects.requireNonNull(attributesByName, "attributesByName must not be null");
        final LinkedHashMap<String, DslSchemaAttribute> attrCopy = new LinkedHashMap<>();
        for (final Map.Entry<String, ? extends DslSchemaAttribute> e : attributesByName.entrySet()) {
            final String key = Objects.requireNonNull(e.getKey(), "attribute name must not be null");
            final DslSchemaAttribute val = Objects.requireNonNull(e.getValue(), "attribute must not be null: " + key);
            if (attrCopy.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate attribute '" + key + "' on element '" + tagName + "'");
            }
            attrCopy.put(key, val);
        }
        this.attributesByName = Collections.unmodifiableMap(attrCopy);

        Objects.requireNonNull(childrenByTag, "childrenByTag must not be null");
        final LinkedHashMap<String, DslType> childCopy = new LinkedHashMap<>();
        for (final Map.Entry<String, ? extends DslType> e : childrenByTag.entrySet()) {
            final String key = Objects.requireNonNull(e.getKey(), "child tag must not be null");
            final DslType val = Objects.requireNonNull(e.getValue(), "child type must not be null: " + key);
            if (childCopy.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate child tag '" + key + "' on element '" + tagName + "'");
            }
            childCopy.put(key, val);
        }
        this.childrenByTag = Collections.unmodifiableMap(childCopy);
    }

    /**
     * @return the element/tag name as used in instance documents.
     */
    public String getTagName() {
        return tagName;
    }

    /**
     * @return immutable map of attribute name -&gt; attribute schema.
     */
    public Map<String, DslSchemaAttribute> getAttributesByName() {
        return attributesByName;
    }

    /**
     * @return attribute schema for a name, or null if not declared on this element.
     */
    public DslSchemaAttribute getAttribute(final String name) {
        return attributesByName.get(name);
    }

    /**
     * @return immutable map of child tag -&gt; child type descriptor.
     */
    public Map<String, DslType> getChildrenByTag() {
        return childrenByTag;
    }

    /**
     * @return child type descriptor for a child tag, or null if not allowed.
     */
    public DslType getChildType(final String childTag) {
        return childrenByTag.get(childTag);
    }
}
