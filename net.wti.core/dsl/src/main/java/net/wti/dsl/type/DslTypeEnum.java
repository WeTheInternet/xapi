package net.wti.dsl.type;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

///
/// DslTypeEnum:
///
/// Schema/type object representing `<enum values=[...]/>` (or future `enum(...)` forms).
/// Values are modeled as a finite set of allowed string literals.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 01:40
public final class DslTypeEnum extends ImmutableDslType {

    private final List<String> values;

    public DslTypeEnum(final MappedIterable<Expression> sourceAst, final List<String> values) {
        super(sourceAst);
        Objects.requireNonNull(values, "values must not be null");
        if (values.isEmpty()) {
            throw new IllegalArgumentException("values must not be empty");
        }
        final ArrayList<String> copy = new ArrayList<>(values.size());
        for (final String v : values) {
            if (v == null || v.isEmpty()) {
                throw new IllegalArgumentException("enum values must be non-empty strings");
            }
            copy.add(v);
        }
        this.values = Collections.unmodifiableList(copy);
    }

    public List<String> getValues() {
        return values;
    }
}
