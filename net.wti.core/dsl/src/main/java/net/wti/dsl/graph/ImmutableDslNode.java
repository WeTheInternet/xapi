package net.wti.dsl.graph;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.Objects;

///
/// ImmutableDslNode:
///
/// Small base class for immutable runtime DslNode implementations.
///
/// Mirrors ImmutableDslType: stores sourceAst for diagnostics/provenance.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 00:20
public abstract class ImmutableDslNode implements DslNode {

    private final MappedIterable<Expression> sourceAst;

    protected ImmutableDslNode(final MappedIterable<Expression> sourceAst) {
        this.sourceAst = Objects.requireNonNull(sourceAst, "sourceAst must not be null");
    }

    @Override
    public final MappedIterable<Expression> getSourceAst() {
        return sourceAst;
    }
}
