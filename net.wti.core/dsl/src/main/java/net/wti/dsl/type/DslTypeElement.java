package net.wti.dsl.type;

import net.wti.lang.parser.ast.expr.Expression;
import xapi.fu.itr.MappedIterable;

import java.util.Objects;

///
/// DslTypeElement:
///
/// element("typeName"): a static-tag element of the named element-type.
///
/// Created by James X. Nelson (James@WeTheInter.net) on 21/12/2025 @ 15:51
public final class DslTypeElement extends ImmutableDslType {

    private final String elementTypeName;

    public DslTypeElement(MappedIterable<Expression> sourceAst, String elementTypeName) {
        super(sourceAst);
        this.elementTypeName = Objects.requireNonNull(elementTypeName, "elementTypeName must not be null");
    }

    public String getElementTypeName() {
        return elementTypeName;
    }
}
