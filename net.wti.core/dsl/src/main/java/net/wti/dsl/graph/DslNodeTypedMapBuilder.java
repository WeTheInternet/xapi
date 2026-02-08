package net.wti.dsl.graph;

import net.wti.dsl.api.DslObject;
import net.wti.dsl.type.DslType;
import net.wti.dsl.type.DslTypeTypedMap;
import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

///
/// DslNodeTypedMapBuilder:
///
/// Mutable builder variant of {@link DslNodeTypedMap}.
///
/// This builder extends {@link AbstractDslNodeBuilder} so it can accumulate source AST references
/// dynamically (for template merges, overlays, etc) via {@link #addSource(Expression)}.
///
/// Schema enforcement:
///  - key restrictions are validated at freeze-time by {@link DslNodeTypedMapImmutable}.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 00:42
public final class DslNodeTypedMapBuilder extends AbstractDslNodeBuilder<DslNodeTypedMapImmutable> implements DslNodeTypedMap {

    private DslTypeTypedMap type;
    private final ArrayList<Entry> entries;

    public DslNodeTypedMapBuilder() {
        super();
        this.entries = new ArrayList<>();
    }

    public DslNodeTypedMapBuilder setType(final DslTypeTypedMap type) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        return this;
    }

    public DslNodeTypedMapBuilder addEntry(final String key, final DslObject value) {
        entries.add(new Entry(key, value));
        return this;
    }

    public DslNodeTypedMapBuilder addEntry(final Entry entry) {
        entries.add(Objects.requireNonNull(entry, "entry must not be null"));
        return this;
    }

    @Override
    public DslTypeTypedMap getType() {
        return type;
    }

    @Override
    public List<Entry> getEntries() {
        return entries;
    }

    @Override
    public DslType getDeclaredType(final String key) {
        return type == null ? null : type.getFieldType(key);
    }

    @Override
    public List<DslObject> getValues(final String key) {
        final ArrayList<DslObject> out = new ArrayList<>();
        for (Entry e : entries) {
            if (key.equals(e.getKey())) {
                out.add(e.getValue());
            }
        }
        return out;
    }

    @Override
    public DslNodeTypedMapImmutable buildImmutable() {
        return new DslNodeTypedMapImmutable(
                getSourceAst(),
                Objects.requireNonNull(type, "type must be set before buildImmutable()"),
                entries
        );
    }
}
