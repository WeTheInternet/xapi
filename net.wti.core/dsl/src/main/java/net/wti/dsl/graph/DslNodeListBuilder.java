package net.wti.dsl.graph;

import net.wti.dsl.api.DslObject;
import net.wti.dsl.type.DslType;
import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

///
/// DslNodeListBuilder:
///
/// Mutable builder variant of {@link DslNodeList}.
///
/// This builder extends {@link AbstractDslNodeBuilder} so it can accumulate source AST references
/// dynamically (for template merges, overlays, etc) via {@link #addSource(Expression)}.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 02:11
public final class DslNodeListBuilder extends AbstractDslNodeBuilder<DslNodeListImmutable> implements DslNodeList {

    private DslType type;
    private final ArrayList<DslObject> items;

    public DslNodeListBuilder() {
        this.items = new ArrayList<>();
    }

    public DslNodeListBuilder setType(final DslType type) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        return this;
    }

    public DslNodeListBuilder addItem(final DslObject item) {
        items.add(Objects.requireNonNull(item, "item must not be null"));
        return this;
    }

    @Override
    public DslType getType() {
        return type;
    }

    @Override
    public List<DslObject> getItems() {
        return items;
    }

    @Override
    public DslNodeListImmutable buildImmutable() {
        return new DslNodeListImmutable(
                getSourceAst(),
                Objects.requireNonNull(type, "type must be set before buildImmutable()"),
                items
        );
    }
}
