package net.wti.dsl.graph;

import net.wti.dsl.api.DslObject;
import net.wti.dsl.type.DslType;
import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

///
/// DslNodeListImmutable:
///
/// Immutable implementation of {@link DslNodeList}.
///
/// This is the “frozen” representation of a list value:
///  - it holds a declared schema {@link DslType},
///  - it holds an ordered, non-null list of {@link DslObject} items,
///  - it retains source AST references for diagnostics/provenance.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 00:42
public final class DslNodeListImmutable extends ImmutableDslNode implements DslNodeList {

    private final DslType type;
    private final List<DslObject> items;

    public DslNodeListImmutable(
            final MappedIterable<Expression> sourceAst,
            final DslType type,
            final List<? extends DslObject> items
    ) {
        super(sourceAst);
        this.type = Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(items, "items must not be null");

        final ArrayList<DslObject> copy = new ArrayList<>(items.size());
        for (final DslObject item : items) {
            copy.add(Objects.requireNonNull(item, "list item must not be null"));
        }
        this.items = Collections.unmodifiableList(copy);
    }

    @Override
    public DslType getType() {
        return type;
    }

    @Override
    public List<DslObject> getItems() {
        return items;
    }
}
