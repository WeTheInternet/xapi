package net.wti.dsl.type;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.Objects;

///
/// DslTypeListOrMap:
///
/// Schema/type object representing `list-or-map`.
/// This is used by existing xapi-dsl schemas to express:
///  - listElement: the type accepted when value is a list
///  - mapValue:    the type accepted when value is a map (key -> value)
///
/// Runtime normalization is deferred; this type is primarily a schema carrier for now.
///
/// TODO: analyze if this type is even needed
///
/// Created by James X. Nelson (James@WeTheInter.net) on 26/12/2025 @ 01:27
public final class DslTypeListOrMap extends ImmutableDslType {

    private final DslType listElementType;
    private final DslType mapValueType;

    public DslTypeListOrMap(
            final MappedIterable<Expression> sourceAst,
            final DslType listElementType,
            final DslType mapValueType
    ) {
        super(sourceAst);
        this.listElementType = Objects.requireNonNull(listElementType, "listElementType must not be null");
        this.mapValueType = Objects.requireNonNull(mapValueType, "mapValueType must not be null");
    }

    public DslType getListElementType() {
        return listElementType;
    }

    public DslType getMapValueType() {
        return mapValueType;
    }
}
