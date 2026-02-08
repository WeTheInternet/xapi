package net.wti.dsl.graph;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.data.ListLike;
import xapi.fu.itr.MappedIterable;
import xapi.fu.java.X_Jdk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

///
/// AbstractDslNodeBuilder:
///
/// Base class for mutable DSL node builders which need to accumulate source AST references
/// dynamically during analysis.
///
/// ## Why this exists
/// During analysis/templating/merging, a single logical node may be derived from multiple AST
/// fragments (for example: local overrides + imported templates). For diagnostics and provenance,
/// we want the final node to retain *all* contributing source expressions, in stable encounter order.
///
/// This base class:
///  - stores an ordered list of contributing {@link Expression} instances,
///  - provides {@link #addSource(Expression)} and {@link #addSources(Iterable)} helpers,
///  - provides a {@link #getSourceAst()} implementation compatible with {@link DslNode}.
///
/// ## Ordering
/// Sources are kept in insertion order. No deduplication is performed, because repeated sources
/// can be meaningful (and equality/identity semantics for AST nodes are not guaranteed to be stable).
///
/// ## Thread safety
/// Builders are not thread-safe. They are intended to be used during single-threaded analysis.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 02:14
public abstract class AbstractDslNodeBuilder<T extends DslNode> implements DslNodeBuilder<T> {

    private final ListLike<Expression> sources;

    protected AbstractDslNodeBuilder() {
        this.sources = X_Jdk.list();
    }

    protected AbstractDslNodeBuilder(final Expression initialSource) {
        this();
        addSource(initialSource);
    }

    /**
     * Adds a single source expression to this builder.
     *
     * @param source the AST expression which contributed to this node (must not be null)
     * @return {@code this} (for chaining)
     */
    @SuppressWarnings("unchecked")
    public final AbstractDslNodeBuilder<T> addSource(final Expression source) {
        sources.add(Objects.requireNonNull(source, "source must not be null"));
        return this;
    }

    /**
     * Adds multiple source expressions to this builder.
     *
     * @param moreSources iterable of expressions; null entries are rejected
     * @return {@code this} (for chaining)
     */
    @SuppressWarnings("unchecked")
    public final AbstractDslNodeBuilder<T> addSources(final Iterable<? extends Expression> moreSources) {
        Objects.requireNonNull(moreSources, "moreSources must not be null");
        for (final Expression e : moreSources) {
            addSource(e);
        }
        return this;
    }

    /**
     * Returns a stable snapshot view of the current sources in insertion order.
     *
     * Note: {@link MappedIterable} is a view type; to avoid exposing the internal mutable list,
     * we snapshot into an unmodifiable list first.
     */
    @Override
    public final MappedIterable<Expression> getSourceAst() {
        return sources;
    }

    /**
     * @return a defensive copy of the current sources (in insertion order).
     */
    protected final ListLike<Expression> snapshotSources() {
        return X_Jdk.listImmutable(sources);
    }
}
