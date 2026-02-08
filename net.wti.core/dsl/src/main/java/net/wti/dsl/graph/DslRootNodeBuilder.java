package net.wti.dsl.graph;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.Objects;

///
/// DslRootNodeBuilder:
///
/// Mutable builder variant of {@link DslRootNode}.
///
/// This builder extends {@link AbstractDslNodeBuilder} so it can accumulate source AST references
/// dynamically (for template merges, overlays, etc) via {@link #addSource(Expression)}.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 00:42
public final class DslRootNodeBuilder extends AbstractDslNodeBuilder<DslRootNodeImmutable> implements DslRootNode {

    private DslNodeElement root;

    public DslRootNodeBuilder() {
        super();
    }

    public DslRootNodeBuilder setRoot(final DslNodeElement root) {
        this.root = Objects.requireNonNull(root, "root must not be null");
        return this;
    }

    @Override
    public DslNodeElement getRoot() {
        return root;
    }

    @Override
    public DslRootNodeImmutable buildImmutable() {
        return new DslRootNodeImmutable(
                getSourceAst(),
                Objects.requireNonNull(root, "root must be set before buildImmutable()")
        );
    }
}
