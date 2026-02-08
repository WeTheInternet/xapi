package net.wti.dsl.graph;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.Objects;

///
/// DslRootNodeImmutable:
///
/// Immutable implementation of {@link DslRootNode}.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 00:42
public final class DslRootNodeImmutable extends ImmutableDslNode implements DslRootNode {

    private final DslNodeElement root;

    public DslRootNodeImmutable(final MappedIterable<Expression> sourceAst, final DslNodeElement root) {
        super(sourceAst);
        this.root = Objects.requireNonNull(root, "root must not be null");
    }

    @Override
    public DslNodeElement getRoot() {
        return root;
    }
}
