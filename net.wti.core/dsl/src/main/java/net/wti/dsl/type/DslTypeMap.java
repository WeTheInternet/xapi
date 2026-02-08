package net.wti.dsl.type;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

///
/// DslTypeMap:
///
/// Schema/type object representing `map(V1, V2, ...)`.
///
/// Notes:
///  - This is a *schema* type.
///  - Runtime values are instruction-lists: ordered entries, repeated keys allowed.
///  - Multiple args imply union for values.
///  - keyType is carried for schema compilation from existing xapi-dsl schemas.
///
public final class DslTypeMap extends ImmutableDslType {

    private final DslType keyType;
    private final List<DslType> valueChoices;

    public DslTypeMap(
            final MappedIterable<Expression> sourceAst,
            final DslType keyType,
            final List<? extends DslType> valueChoices
    ) {
        super(sourceAst);
        this.keyType = Objects.requireNonNull(keyType, "keyType must not be null");
        Objects.requireNonNull(valueChoices, "valueChoices must not be null");
        this.valueChoices = Collections.unmodifiableList(new ArrayList<>(valueChoices));
    }

    public DslType getKeyType() {
        return keyType;
    }

    public List<DslType> getValueChoices() {
        return valueChoices;
    }
}
