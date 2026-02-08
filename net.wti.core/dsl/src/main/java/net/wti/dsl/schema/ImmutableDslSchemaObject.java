package net.wti.dsl.schema;

import net.wti.dsl.api.DslObject;
import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.Objects;

///
/// ImmutableDslSchemaObject:
///
/// Base class for immutable schema objects produced by compiling a DSL definition.
///
/// Schema objects mirror the runtime node/value philosophy:
/// they retain one-or-more source AST expressions for diagnostics, provenance,
/// and generator error reporting.
///
/// This base type is deliberately small:
///  - it stores {@link #getSourceAst()},
///  - it does not impose any visitor or reflection mechanics.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 03:00
public abstract class ImmutableDslSchemaObject implements DslObject {

    private final MappedIterable<Expression> sourceAst;

    protected ImmutableDslSchemaObject(final MappedIterable<Expression> sourceAst) {
        this.sourceAst = Objects.requireNonNull(sourceAst, "sourceAst must not be null");
    }

    @Override
    public final MappedIterable<Expression> getSourceAst() {
        return sourceAst;
    }
}
