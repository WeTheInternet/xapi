package net.wti.dsl.schema;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

///
/// DslSchema:
///
/// The compiled, immutable schema graph for a DSL definition.
///
/// This is the “static truth” produced after parsing and compiling a <xapi-dsl .../> document:
///  - it contains all declared elements,
///  - it contains a reusable attribute registry (top-level),
///  - it records the configured root tag,
///  - it retains source provenance.
///
/// ## Why attributes are top-level *and* per-element
/// - Top-level attributes enable reuse-by-name across elements and simplify generator wiring.
/// - Per-element maps provide the precise allowed/required/default set for a given tag.
///
/// In the compiled schema, elements typically point to the *same* attribute objects as the
/// top-level registry, but this is not strictly required (element-specific attributes are allowed).
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 03:12
public final class DslSchema extends ImmutableDslSchemaObject {

    private final String name;
    private final String packageName;
    private final String rootTag;

    private final Map<String, DslSchemaAttribute> attributes;
    private final Map<String, DslSchemaElement> elements;

    public DslSchema(
            final MappedIterable<Expression> sourceAst,
            final String name,
            final String packageName,
            final String rootTag,
            final Map<String, ? extends DslSchemaAttribute> attributes,
            final Map<String, ? extends DslSchemaElement> elements
    ) {
        super(sourceAst);
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name must not be null/empty");
        }
        if (packageName == null || packageName.isEmpty()) {
            throw new IllegalArgumentException("packageName must not be null/empty");
        }
        if (rootTag == null || rootTag.isEmpty()) {
            throw new IllegalArgumentException("rootTag must not be null/empty");
        }
        this.name = name;
        this.packageName = packageName;
        this.rootTag = rootTag;

        Objects.requireNonNull(attributes, "attributes must not be null");
        final LinkedHashMap<String, DslSchemaAttribute> attrCopy = new LinkedHashMap<>();
        for (final Map.Entry<String, ? extends DslSchemaAttribute> e : attributes.entrySet()) {
            final String key = Objects.requireNonNull(e.getKey(), "attribute name must not be null");
            final DslSchemaAttribute val = Objects.requireNonNull(e.getValue(), "attribute must not be null: " + key);
            if (attrCopy.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate schema attribute '" + key + "'");
            }
            attrCopy.put(key, val);
        }
        this.attributes = Collections.unmodifiableMap(attrCopy);

        Objects.requireNonNull(elements, "elements must not be null");
        final LinkedHashMap<String, DslSchemaElement> elCopy = new LinkedHashMap<>();
        for (final Map.Entry<String, ? extends DslSchemaElement> e : elements.entrySet()) {
            final String key = Objects.requireNonNull(e.getKey(), "element tag must not be null");
            final DslSchemaElement val = Objects.requireNonNull(e.getValue(), "element must not be null: " + key);
            if (elCopy.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate element '" + key + "'");
            }
            elCopy.put(key, val);
        }
        this.elements = Collections.unmodifiableMap(elCopy);
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getRootTag() {
        return rootTag;
    }

    /**
     * @return top-level reusable attribute registry (by name).
     */
    public Map<String, DslSchemaAttribute> getAttributes() {
        return attributes;
    }

    /**
     * @return top-level attribute schema, or null if not defined.
     */
    public DslSchemaAttribute getAttribute(final String name) {
        return attributes.get(name);
    }

    /**
     * @return element registry (by tag name).
     */
    public Map<String, DslSchemaElement> getElements() {
        return elements;
    }

    /**
     * @return element schema for a tag, or null if not defined.
     */
    public DslSchemaElement getElement(final String tagName) {
        return elements.get(tagName);
    }
}
