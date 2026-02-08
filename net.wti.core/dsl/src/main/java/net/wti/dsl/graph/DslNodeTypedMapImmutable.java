package net.wti.dsl.graph;

import net.wti.dsl.api.DslObject;
import net.wti.dsl.type.DslType;
import net.wti.dsl.type.DslTypeTypedMap;
import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

///
/// DslNodeTypedMapImmutable:
///
/// Immutable implementation of {@link DslNodeTypedMap}.
///
/// Semantics (per design docs):
///  - runtime typedMap values are an instruction-list: ordered entries, repeated keys allowed
///  - keys are restricted to the schema's declared field set
///  - values are normalized {@link DslObject} instances (DslValue or nested DslNode)
///
/// Invariants:
///  - {@code type} is non-null
///  - {@code entries} is non-null and contains no null entries
///  - every entry key is declared by {@code type}
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 00:20
public final class DslNodeTypedMapImmutable extends ImmutableDslNode implements DslNodeTypedMap {

    private final DslTypeTypedMap type;
    private final List<Entry> entries;

    public DslNodeTypedMapImmutable(
            final MappedIterable<Expression> sourceAst,
            final DslTypeTypedMap type,
            final List<? extends Entry> entries
    ) {
        super(sourceAst);
        this.type = Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(entries, "entries must not be null");

        final ArrayList<Entry> copy = new ArrayList<>(entries.size());
        for (Entry e : entries) {
            Objects.requireNonNull(e, "entry must not be null");
            if (!type.hasField(e.getKey())) {
                throw new IllegalArgumentException(
                        "typedMap entry key '" + e.getKey() + "' is not declared in schema; expected one of " +
                                type.getFieldTypes().keySet()
                );
            }
            copy.add(e);
        }
        this.entries = Collections.unmodifiableList(copy);
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
        return type.getFieldType(key);
    }

    @Override
    public List<DslObject> getValues(final String key) {
        if (!type.hasField(key)) {
            return Collections.emptyList();
        }
        final ArrayList<DslObject> out = new ArrayList<>();
        for (Entry e : entries) {
            if (key.equals(e.getKey())) {
                out.add(e.getValue());
            }
        }
        return Collections.unmodifiableList(out);
    }
}
