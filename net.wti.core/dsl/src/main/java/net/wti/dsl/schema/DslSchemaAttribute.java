package net.wti.dsl.schema;

import net.wti.dsl.type.DslType;
import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.Objects;

///
/// DslSchemaAttribute:
///
/// A compiled schema declaration for a reusable attribute definition.
///
/// Attributes are top-level in {@link DslSchema} so they can be reused across
/// multiple elements by name (and so generators have a canonical place to look
/// for cross-element attribute shape).
///
/// Each element also contains its own attribute map; those entries can either:
///  - reference the same {@link DslSchemaAttribute} instance (typical), or
///  - be element-specific (allowed, but discouraged unless intentionally overriding).
///
/// ## Required/default
/// This type captures both:
///  - schema *shape* ({@link #getType()}),
///  - schema *constraints/metadata* ({@link #isRequired()}, {@link #getDefaultValueExpr()}).
///
/// Default values are stored as raw AST expressions because:
///  - they may need DSL-specific interpretation,
///  - generators may want to embed them directly or re-parse later.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 03:11
public final class DslSchemaAttribute extends ImmutableDslSchemaObject {

    private final String name;
    private final DslType type;
    private final boolean required;
    private final Expression defaultValueExpr;

    public DslSchemaAttribute(
            final MappedIterable<Expression> sourceAst,
            final String name,
            final DslType type,
            final boolean required,
            final Expression defaultValueExpr
    ) {
        super(sourceAst);
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name must not be null/empty");
        }
        this.name = name;
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.required = required;
        this.defaultValueExpr = defaultValueExpr;
    }

    /**
     * @return the declared attribute name as used in xapi source.
     */
    public String getName() {
        return name;
    }

    /**
     * @return the attribute value shape/type descriptor.
     */
    public DslType getType() {
        return type;
    }

    /**
     * @return true if this attribute is required by the schema.
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * @return raw AST expression for the default value, or null if none is declared.
     */
    public Expression getDefaultValueExpr() {
        return defaultValueExpr;
    }
}
